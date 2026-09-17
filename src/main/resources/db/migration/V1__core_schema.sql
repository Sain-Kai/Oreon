CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE app_users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    username      VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE documents (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    filename       VARCHAR(500) NOT NULL,
    content_type   VARCHAR(150),
    extracted_text TEXT,
    status         VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    error_message  TEXT,
    uploaded_by    UUID REFERENCES app_users(id),
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_tenant ON documents(tenant_id);
CREATE INDEX idx_documents_status ON documents(status);

CREATE TABLE query_logs (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    user_id             UUID REFERENCES app_users(id),
    question            TEXT NOT NULL,
    answer              TEXT,
    source_document_ids TEXT,
    latency_ms          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_query_logs_tenant ON query_logs(tenant_id);
