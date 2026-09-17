package com.ragplatform.audit;

import com.ragplatform.config.KafkaTopicConfig;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private final QueryLogRepository queryLogRepository;

    public AuditEventListener(QueryLogRepository queryLogRepository) {
        this.queryLogRepository = queryLogRepository;
    }

    @KafkaListener(topics = KafkaTopicConfig.AUDIT_EVENTS, containerFactory = "kafkaListenerContainerFactory")
    public void handle(QueryLoggedEvent event) {
        queryLogRepository.save(new QueryLog(
                event.tenantId(), event.userId(), event.question(), event.answer(),
                event.sourceDocumentIds(), event.latencyMs()));
    }
}
