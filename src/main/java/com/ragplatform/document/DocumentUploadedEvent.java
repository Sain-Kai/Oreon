package com.ragplatform.document;

import java.util.UUID;

public record DocumentUploadedEvent(UUID documentId, UUID tenantId) {
}
