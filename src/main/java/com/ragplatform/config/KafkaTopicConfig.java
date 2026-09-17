package com.ragplatform.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String DOCUMENT_EVENTS = "document-events";
    public static final String DOCUMENT_EVENTS_DLT = "document-events.DLT";
    public static final String AUDIT_EVENTS = "audit-events";

    @Bean
    public NewTopic documentEventsTopic() {
        return TopicBuilder.name(DOCUMENT_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic documentEventsDltTopic() {
        return TopicBuilder.name(DOCUMENT_EVENTS_DLT).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(AUDIT_EVENTS).partitions(3).replicas(1).build();
    }
}
