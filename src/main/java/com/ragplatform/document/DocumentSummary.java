package com.ragplatform.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentSummary(UUID id, String filename, DocumentStatus status, String errorMessage, Instant createdAt) {
    public static DocumentSummary from(DocumentRecord d) {
        return new DocumentSummary(d.getId(), d.getFilename(), d.getStatus(), d.getErrorMessage(), d.getCreatedAt());
    }
}
