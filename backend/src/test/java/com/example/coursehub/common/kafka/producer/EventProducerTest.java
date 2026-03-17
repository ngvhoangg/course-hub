package com.example.coursehub.common.kafka.producer;

import com.example.coursehub.common.kafka.Topics;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EventProducer eventProducer;

    @Test
    void sendEmailVerification_shouldPublishToCorrectTopic() {
        EmailVerificationEvent event = new EmailVerificationEvent("user@example.com", "token-123");

        eventProducer.sendEmailVerification(event);

        verify(kafkaTemplate).send(Topics.EMAIL_VERIFICATION, event);
    }

    @Test
    void sendEmailVerification_shouldPublishWithCorrectEvent() {
        EmailVerificationEvent event = new EmailVerificationEvent("user@example.com", "token-123");

        eventProducer.sendEmailVerification(event);

        verify(kafkaTemplate).send(eq(Topics.EMAIL_VERIFICATION), eq(event));
    }
}