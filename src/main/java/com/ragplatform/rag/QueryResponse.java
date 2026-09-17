package com.ragplatform.rag;

import java.util.List;

public record QueryResponse(String answer, List<SourceCitation> citations) {
}
