package com.ragplatform.audit;

import java.util.UUID;

public record QueryLoggedEvent(
        UUID tenantId, UUID userId, String question, String answer, String sourceDocumentIds, long latencyMs) {
}
