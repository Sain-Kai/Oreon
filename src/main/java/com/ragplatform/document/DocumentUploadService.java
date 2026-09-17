package com.ragplatform.document;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentUploadService {

    private final DocumentRepository documentRepository;
    private final DocumentEventProducer eventProducer;
    private final Tika tika = new Tika();

    public DocumentUploadService(DocumentRepository documentRepository, DocumentEventProducer eventProducer) {
        this.documentRepository = documentRepository;
        this.eventProducer = eventProducer;
    }

    /**
     * Text extraction (Tika) happens synchronously here because it's fast and local - no external
     * API call involved. The slow, rate-limited part (chunking + calling the embedding API) is
     * pushed onto Kafka so the upload endpoint responds immediately instead of making the caller
     * wait on an LLM provider.
     */
    public UUID upload(MultipartFile file, UUID tenantId, UUID uploadedBy) {
        String extractedText;
        try {
            extractedText = tika.parseToString(file.getInputStream());
        } catch (IOException | org.apache.tika.exception.TikaException e) {
            throw new IllegalArgumentException("Could not read file contents: " + e.getMessage(), e);
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException("No extractable text found in file");
        }

        DocumentRecord record = new DocumentRecord(
                tenantId, file.getOriginalFilename(), file.getContentType(), extractedText, uploadedBy);
        documentRepository.save(record);

        eventProducer.publish(new DocumentUploadedEvent(record.getId(), tenantId));
        return record.getId();
    }
}
