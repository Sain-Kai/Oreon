package com.ragplatform.document;

import com.ragplatform.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Consumes document-events, chunks the extracted text, and hands it to the vector store - which
 * calls the configured EmbeddingModel internally to actually compute and store the embeddings.
 * This is the step that talks to the (rate-limited, free-tier) embedding API, which is exactly why
 * it's decoupled from the synchronous upload request via Kafka in the first place.
 */
@Component
public class DocumentProcessingListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingListener.class);

    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public DocumentProcessingListener(DocumentRepository documentRepository, VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
    }

    @KafkaListener(topics = KafkaTopicConfig.DOCUMENT_EVENTS, containerFactory = "kafkaListenerContainerFactory")
    public void handle(DocumentUploadedEvent event) {
        DocumentRecord doc = documentRepository.findByIdAndTenantId(event.documentId(), event.tenantId())
                .orElseThrow(() -> new NoSuchElementException("Document " + event.documentId() + " not found"));

        try {
            doc.markProcessing();
            documentRepository.save(doc);

            Document sourceDoc = new Document(doc.getExtractedText(), Map.of(
                    "tenantId", doc.getTenantId().toString(),
                    "documentId", doc.getId().toString(),
                    "filename", doc.getFilename()
            ));

            List<Document> chunks = splitter.apply(List.of(sourceDoc));
            vectorStore.add(chunks);

            doc.markReady();
            documentRepository.save(doc);
            log.info("Document {} processed into {} chunks", doc.getId(), chunks.size());

        } catch (Exception e) {
            log.error("Failed to process document {}: {}", doc.getId(), e.getMessage(), e);
            doc.markFailed(e.getMessage());
            documentRepository.save(doc);
            throw e; // rethrow so the error handler's retry/DLT policy takes over
        }
    }
}
