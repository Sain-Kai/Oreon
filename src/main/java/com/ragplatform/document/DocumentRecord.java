package com.ragplatform.document;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type")
    private String contentType;

    @Lob
    @Column(name = "extracted_text")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DocumentRecord() {
    }

    public DocumentRecord(UUID tenantId, String filename, String contentType, String extractedText, UUID uploadedBy) {
        this.tenantId = tenantId;
        this.filename = filename;
        this.contentType = contentType;
        this.extractedText = extractedText;
        this.uploadedBy = uploadedBy;
    }

    public void markProcessing() {
        this.status = DocumentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markReady() {
        this.status = DocumentStatus.READY;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = error;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public String getExtractedText() { return extractedText; }
    public DocumentStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public UUID getUploadedBy() { return uploadedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
