package com.ragplatform.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "query_logs")
public class QueryLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "source_document_ids")
    private String sourceDocumentIds;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected QueryLog() {
    }

    public QueryLog(UUID tenantId, UUID userId, String question, String answer, String sourceDocumentIds, Long latencyMs) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.sourceDocumentIds = sourceDocumentIds;
        this.latencyMs = latencyMs;
    }
}
