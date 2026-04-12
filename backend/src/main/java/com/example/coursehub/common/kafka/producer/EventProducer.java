package com.example.coursehub.common.kafka.producer;

import com.example.coursehub.common.kafka.Topics;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
import com.example.coursehub.common.kafka.event.EntitySyncEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEmailVerification(EmailVerificationEvent event) {
        kafkaTemplate.send(Topics.EMAIL_VERIFICATION, event);
        log.info("Published EmailVerificationEvent for: {}", event.toEmail());
    }

    public void sendEntitySyncEvent(EntitySyncEvent event) {
        String key = event.entityType() + "-" + event.entityId();
        kafkaTemplate.send(Topics.ENTITY_SYNC, key, event);
        log.info("Published sync event for {} ID: {}", event.entityType(), event.entityId());
    }
}
