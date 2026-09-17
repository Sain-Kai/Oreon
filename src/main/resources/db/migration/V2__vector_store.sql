CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;

-- IMPORTANT: the vector dimension below MUST match your embedding model's output size exactly,
-- and must match app.EMBEDDING_DIMENSIONS / spring.ai.vectorstore.pgvector.dimensions in application.yml.
-- 2048 is a placeholder - check your chosen embedding model's docs (e.g. the OpenRouter model page)
-- and edit this file (and the property) BEFORE the first migration ever runs against a real database.
-- Changing it later requires a new migration that drops and recreates this table (embeddings are not portable
-- across dimension sizes), so get it right up front.
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(2048)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING HNSW (embedding vector_cosine_ops);
