# RAG Platform

Multi-tenant document intelligence platform: upload documents, ask grounded questions with source
citations. Spring Boot + Spring AI, async ingestion via Kafka, Redis caching + rate limiting,
Postgres/pgvector for storage, deployable via Docker Compose or Kubernetes.

## Architecture

- **Chat model:** NVIDIA's hosted API (`integrate.api.nvidia.com`), OpenAI-compatible, free tier.
- **Embedding model:** OpenRouter (free tier) - kept separate from NVIDIA because NVIDIA's
  retrieval-tuned embedding models need an `input_type` field the standard OpenAI embeddings
  request doesn't support; see `EmbeddingConfig.java`.
- **Fallback model:** a second free OpenRouter chat model, used only if NVIDIA fails after retries -
  see `ResilientChatService.java`.
- **Ingestion:** upload → synchronous text extraction (Apache Tika) → Kafka event → async
  chunk + embed + store in pgvector. Decoupled so the upload endpoint never blocks on a rate-limited
  external API call.
- **Multi-tenancy:** shared schema, every table has a `tenant_id`, every query is scoped to the
  tenant pulled from the JWT (`TenantContext`) - never trusted from client input.
- **Citations:** retrieval is done manually (`vectorStore.similaritySearch`) rather than via Spring
  AI's `QuestionAnswerAdvisor`, specifically so the exact retrieved chunks are available to build
  citations from - see the comment in `RagQueryService.java`.
- **Audit trail:** every query + answer is logged asynchronously via a separate Kafka topic.

## Before you run this anywhere

A few things need your attention first - I can't verify these without your actual accounts/keys:

1. **API keys** - get an NVIDIA key at [build.nvidia.com](https://build.nvidia.com) and an
   OpenRouter key at [openrouter.ai](https://openrouter.ai). Set them as described below. Don't
   commit real keys anywhere in this repo.
2. **Exact model slugs** - `NVIDIA_CHAT_MODEL`, `OPENROUTER_EMBEDDING_MODEL`, and
   `OPENROUTER_FALLBACK_CHAT_MODEL` in `.env.example` are my best guesses at working free-tier
   model IDs as of when this was written. Copy the *exact* current slug from each model's page
   before you rely on it - these catalogs change.
3. **Embedding dimension** - `EMBEDDING_DIMENSIONS` and the `vector(2048)` column in
   `V2__vector_store.sql` MUST match your chosen embedding model's actual output size exactly, or
   every embed call will fail. Check the model's docs and fix both places (they must agree) before
   the first migration ever runs against a real database - changing it later means dropping and
   re-embedding everything.
4. **Spring AI version drift** - Spring AI is a fast-moving framework. The `spring-ai-bom` version
   in `pom.xml` is pinned to what was current when this was built; check
   [mvnrepository](https://mvnrepository.com/artifact/org.springframework.ai/spring-ai-bom) or
   start.spring.io for the current stable release before your first build, and check the
   [migration notes](https://docs.spring.io/spring-ai/reference/) if any AI-related class doesn't
   resolve.
5. **This wasn't compiled in a sandbox** - I don't have Maven Central access in the environment I
   built this in, so I couldn't run `mvn verify` to confirm everything compiles clean. The code
   follows well-documented Spring Boot/Spring AI patterns, but there's a real chance of a small
   typo or API mismatch surfacing on your first build. Paste me the error and I'll fix it fast.

## Environment variables

| Variable | Where it's used | Notes |
|---|---|---|
| `NVIDIA_API_KEY` | chat model | from build.nvidia.com |
| `NVIDIA_CHAT_MODEL` | chat model | e.g. `meta/llama-3.1-70b-instruct` |
| `OPENROUTER_API_KEY` | embeddings + fallback chat | from openrouter.ai |
| `OPENROUTER_EMBEDDING_MODEL` | embeddings | e.g. `nvidia/nemotron-3-embed-1b:free` |
| `OPENROUTER_FALLBACK_CHAT_MODEL` | fallback chat | e.g. `z-ai/glm-5.2:free` |
| `JWT_SECRET` | auth | generate with `openssl rand -base64 48` |
| `EMBEDDING_DIMENSIONS` | pgvector schema | must match your embedding model exactly |
| `DB_PASSWORD` / `DB_USERNAME` / `DB_URL` | Postgres | |
| `RATE_LIMIT_RPM` | per-tenant rate limit | default 20/min |

**Set these yourself, locally, before deploying** - copy `.env.example` to `.env` for
docker-compose, or as a Kubernetes Secret for a cluster (see `k8s/02-secret-template.yaml`), or as
GitHub Actions repo secrets for CI/CD (`Settings → Secrets and variables → Actions`). Nothing in
this repo should ever contain a real key.

## Running locally (docker-compose)

```bash
cp .env.example .env    # fill in your real values
docker compose up --build
```

This starts Postgres (with pgvector), Redis, a single-node Kafka broker (KRaft mode, no
Zookeeper needed), and the app itself. First boot will take a minute while Flyway migrates the
schema and Kafka finishes electing itself as its own controller.

Try it:

```bash
# register a tenant + admin user
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"tenantName":"Acme","username":"admin","password":"changeme123"}'

# upload a document (use the token from the response above)
curl -X POST localhost:8080/api/documents \
  -H "Authorization: Bearer <token>" -F "file=@somefile.pdf"

# ask a question once processing finishes (check status via GET /api/documents/{id})
curl -X POST localhost:8080/api/query \
  -H "Authorization: Bearer <token>" -H 'Content-Type: application/json' \
  -d '{"question":"What does this document say about X?"}'
```

## Running on Kubernetes (local demo via kind/minikube)

**Reality check first:** Render (your free hosting target) doesn't run raw Kubernetes - it's a
PaaS, not a cluster host. So these manifests are for local demonstration (`kind` or `minikube`) or
a real cluster if you get access to one - genuinely useful to show in an interview/resume even if
they're not what serves your live public demo link. For the actual public deployment, use the
`deploy-render` job in the CI/CD pipeline instead.

```bash
kind create cluster --name rag-platform
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap.yaml
kubectl create secret generic rag-platform-secrets -n rag-platform \
  --from-literal=DB_PASSWORD='ragpass' \
  --from-literal=NVIDIA_API_KEY='your-key' \
  --from-literal=OPENROUTER_API_KEY='your-key' \
  --from-literal=JWT_SECRET="$(openssl rand -base64 48)"
kubectl apply -f k8s/
kubectl get pods -n rag-platform -w
```

Update the image in `k8s/20-app-deployment.yaml` to point at your actual pushed image
(`ghcr.io/<your-username>/rag-platform:latest`) before applying.

## CI/CD

`.github/workflows/ci-cd.yml`: runs tests (`mvn verify`, including the Testcontainers-based
integration test) on every push/PR, builds and pushes a Docker image to GitHub Container Registry
on merge to `main`, then triggers a Render deploy via a deploy-hook URL (add
`RENDER_DEPLOY_HOOK_URL` as a repo secret - found in your Render service's settings). A commented-out
`deploy-k8s` job shows how you'd extend this to a real cluster once you have one.

## What's deliberately left for you to extend

- Only a couple of representative tests are included (JWT round-trip, tenant-scoped repository
  query, upload service logic) - not full coverage. Extend the Testcontainers pattern in
  `DocumentRepositoryIntegrationTest` for more.
- No object storage (S3-style) for original files - extracted text is stored directly in Postgres,
  which is fine at portfolio scale but not how you'd do it with large files at real scale.
- No admin/multi-user-per-tenant flow yet (invite teammates into an existing tenant) - `/register`
  currently always creates a brand-new tenant.
- A custom `EmbeddingModel` that calls NVIDIA's embedding endpoint directly (handling the
  `input_type` field properly) instead of routing embeddings through OpenRouter - would let you run
  entirely on NVIDIA. Good next step once the OpenRouter path is working end-to-end.
