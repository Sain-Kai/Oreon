package com.ragplatform.document;

import com.ragplatform.tenant.Tenant;
import com.ragplatform.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a real Postgres (via Testcontainers, running the actual pgvector image) rather than H2, since
 * the schema depends on Postgres-specific extensions (uuid-ossp, vector) that H2 can't emulate.
 * Flyway runs its normal migrations against this container before the test executes.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // keep the app from trying to reach real external APIs during this test
        registry.add("spring.ai.openai.api-key", () -> "test-key");
        registry.add("app.openrouter.api-key", () -> "test-key");
        registry.add("app.jwt.secret", () -> "test-secret-key-that-is-long-enough-for-hs256-signing");
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void savesAndScopesDocumentsByTenant() {
        Tenant tenantA = tenantRepository.save(new Tenant("Tenant A"));
        Tenant tenantB = tenantRepository.save(new Tenant("Tenant B"));

        documentRepository.save(new DocumentRecord(tenantA.getId(), "a.pdf", "application/pdf", "content a", null));
        documentRepository.save(new DocumentRecord(tenantB.getId(), "b.pdf", "application/pdf", "content b", null));

        List<DocumentRecord> tenantADocs = documentRepository.findAllByTenantId(tenantA.getId());

        assertThat(tenantADocs).hasSize(1);
        assertThat(tenantADocs.get(0).getFilename()).isEqualTo("a.pdf");
        assertThat(documentRepository.findByIdAndTenantId(tenantADocs.get(0).getId(), tenantB.getId())).isEmpty();
    }
}
