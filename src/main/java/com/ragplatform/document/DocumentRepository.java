package com.ragplatform.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentRecord, UUID> {
    Optional<DocumentRecord> findByIdAndTenantId(UUID id, UUID tenantId);
    List<DocumentRecord> findAllByTenantId(UUID tenantId);
}
