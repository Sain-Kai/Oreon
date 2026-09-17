package com.ragplatform.document;

import java.util.UUID;

public record DocumentUploadResponse(UUID documentId, String status) {
}
