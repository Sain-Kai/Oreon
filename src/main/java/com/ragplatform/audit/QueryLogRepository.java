package com.ragplatform.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QueryLogRepository extends JpaRepository<QueryLog, UUID> {
}
