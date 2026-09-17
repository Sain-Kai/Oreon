package com.ragplatform.document;

import com.ragplatform.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentUploadService uploadService;
    private final DocumentRepository documentRepository;

    public DocumentController(DocumentUploadService uploadService, DocumentRepository documentRepository) {
        this.uploadService = uploadService;
        this.documentRepository = documentRepository;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        UUID documentId = uploadService.upload(file, TenantContext.tenantId(), TenantContext.userId());
        return ResponseEntity.accepted().body(new DocumentUploadResponse(documentId, "PENDING"));
    }

    @GetMapping
    public List<DocumentSummary> list() {
        return documentRepository.findAllByTenantId(TenantContext.tenantId()).stream()
                .map(DocumentSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentSummary get(@PathVariable UUID id) {
        DocumentRecord doc = documentRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .orElseThrow(() -> new NoSuchElementException("Document not found"));
        return DocumentSummary.from(doc);
    }
}
