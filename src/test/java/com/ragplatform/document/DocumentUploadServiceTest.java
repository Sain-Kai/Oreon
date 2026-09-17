package com.ragplatform.document;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentUploadServiceTest {

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentEventProducer eventProducer = mock(DocumentEventProducer.class);
    private final DocumentUploadService service = new DocumentUploadService(documentRepository, eventProducer);

    @Test
    void extractsTextAndPublishesEventOnUpload() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello world".getBytes());

        when(documentRepository.save(org.mockito.ArgumentMatchers.any(DocumentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.upload(file, tenantId, userId);

        ArgumentCaptor<DocumentUploadedEvent> captor = ArgumentCaptor.forClass(DocumentUploadedEvent.class);
        verify(eventProducer).publish(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenantId);
    }

    @Test
    void rejectsFilesWithNoExtractableText() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.upload(emptyFile, UUID.randomUUID(), UUID.randomUUID()));
    }
}
