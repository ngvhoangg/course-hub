package com.example.coursehub.common.kafka.consumer;

import com.example.coursehub.common.email.EmailService;
import com.example.coursehub.common.kafka.ConsumerGroups;
import com.example.coursehub.common.kafka.Topics;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailConsumer {
    private final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RetryableTopic(
        attempts = "4",                                    // 1 original + 3 retries
        backoff = @Backoff(delay = 3000, multiplier = 2), // 3s -> 6s -> 12s
        autoCreateTopics = "true",
        exclude = {IllegalArgumentException.class,         // don't retry on bad input
            IllegalStateException.class},                  // don't retry on config errors
        dltStrategy = DltStrategy.FAIL_ON_ERROR

    )
    @KafkaListener(topics = Topics.EMAIL_VERIFICATION, groupId = ConsumerGroups.EMAIL)
    public void handleEmailVerification(EmailVerificationEvent event) {
        log.info("Consumed EmailVerificationEvent for: {}", event.toEmail());
        emailService.sendVerificationEmail(event.toEmail(), event.token());
    }

    @DltHandler
    public void handleDlt(EmailVerificationEvent event) {
        log.error("Email verification failed after all retries for: {}", event.toEmail());
        // future: send alert, store in DB for manual reprocess
    }
}
