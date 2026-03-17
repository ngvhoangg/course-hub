package com.example.coursehub.common.kafka.consumer;

import com.example.coursehub.common.email.EmailService;
import com.example.coursehub.common.kafka.ConsumerGroups;
import com.example.coursehub.common.kafka.Topics;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailConsumer {
    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = Topics.EMAIL_VERIFICATION, groupId = ConsumerGroups.EMAIL)
    public void handleEmailVerification(EmailVerificationEvent event) {
        log.info("Consumed EmailVerificationEvent for: {}", event.toEmail());
        emailService.sendVerificationEmail(event.toEmail(), event.token());
    }
}
