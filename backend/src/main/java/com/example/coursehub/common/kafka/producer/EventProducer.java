package com.example.coursehub.common.kafka.producer;

import com.example.coursehub.common.kafka.Topics;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
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
}
