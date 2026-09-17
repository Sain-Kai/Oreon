package com.ragplatform.audit;

import com.ragplatform.config.KafkaTopicConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuditEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(QueryLoggedEvent event) {
        // audit logging is decoupled from the query response path via Kafka - a slow or briefly
        // unavailable DB write should never make the user wait longer for their answer
        kafkaTemplate.send(KafkaTopicConfig.AUDIT_EVENTS, event.tenantId().toString(), event);
    }
}
