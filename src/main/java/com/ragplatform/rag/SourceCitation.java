package com.ragplatform.rag;

import java.util.UUID;

public record SourceCitation(UUID documentId, String filename, String snippet) {
}
