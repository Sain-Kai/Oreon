package com.ragplatform.rag;

import com.ragplatform.audit.AuditEventProducer;
import com.ragplatform.audit.QueryLoggedEvent;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deliberately does retrieval manually (vectorStore.similaritySearch + a hand-built system prompt)
 * rather than via Spring AI's QuestionAnswerAdvisor. Functionally similar, but this way the exact
 * retrieved documents are in hand for building citations - the advisor's internal retrieval context
 * key has moved between Spring AI releases, and citations are worth not leaving to chance.
 */
@Service
public class RagQueryService {

    private static final int TOP_K = 5;

    private final VectorStore vectorStore;
    private final ResilientChatService chatService;
    private final AuditEventProducer auditEventProducer;

    public RagQueryService(VectorStore vectorStore, ResilientChatService chatService,
                            AuditEventProducer auditEventProducer) {
        this.vectorStore = vectorStore;
        this.chatService = chatService;
        this.auditEventProducer = auditEventProducer;
    }

    @Cacheable(value = "ragQueries", key = "#tenantId + ':' + #question")
    public QueryResponse answer(UUID tenantId, UUID userId, String question) {
        long start = System.currentTimeMillis();

        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(TOP_K)
                .filterExpression("tenantId == '" + tenantId + "'")
                .build();

        List<Document> retrieved = vectorStore.similaritySearch(searchRequest);

        if (retrieved.isEmpty()) {
            QueryResponse response = new QueryResponse(
                    "I don't have any documents to answer that from yet. Upload some first.", List.of());
            logAsync(tenantId, userId, question, response, start);
            return response;
        }

        String context = retrieved.stream()
                .map(d -> "Source [%s]: %s".formatted(d.getMetadata().get("filename"), d.getText()))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
                You are a document assistant for this organization. Answer the user's question using
                ONLY the context below. If the answer isn't in the context, say plainly that you don't
                have enough information - never make something up. Cite which source you used when relevant.

                Context:
                %s
                """.formatted(context);

        String answer = chatService.answer(systemPrompt, question);

        List<SourceCitation> citations = retrieved.stream()
                .map(d -> new SourceCitation(
                        UUID.fromString(String.valueOf(d.getMetadata().get("documentId"))),
                        String.valueOf(d.getMetadata().get("filename")),
                        snippet(d.getText())))
                .distinct()
                .toList();

        QueryResponse response = new QueryResponse(answer, citations);
        logAsync(tenantId, userId, question, response, start);
        return response;
    }

    private void logAsync(UUID tenantId, UUID userId, String question, QueryResponse response, long startMs) {
        long latency = System.currentTimeMillis() - startMs;
        String sourceIds = response.citations().stream()
                .map(c -> c.documentId().toString())
                .distinct()
                .collect(Collectors.joining(","));
        auditEventProducer.publish(new QueryLoggedEvent(tenantId, userId, question, response.answer(), sourceIds, latency));
    }

    private String snippet(String text) {
        return text.length() > 220 ? text.substring(0, 220) + "..." : text;
    }
}
