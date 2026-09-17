package com.ragplatform.document;

import com.ragplatform.config.KafkaTopicConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DocumentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DocumentUploadedEvent event) {
        // keyed by tenantId so all of one tenant's document events land on the same partition,
        // preserving per-tenant ordering without needing global ordering across the whole topic
        kafkaTemplate.send(KafkaTopicConfig.DOCUMENT_EVENTS, event.tenantId().toString(), event);
    }
}
