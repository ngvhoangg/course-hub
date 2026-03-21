package com.example.coursehub.common.kafka.consumer;

import com.example.coursehub.common.email.EmailService;
import com.example.coursehub.common.kafka.Topics;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.kafka.listener.auto-startup=true"
})
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class EmailConsumerTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static ConfluentKafkaContainer kafka =
        new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private EmailService emailService;

    @MockitoSpyBean
    private EmailConsumer emailConsumer;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(emailService, emailConsumer);
    }

    @Test
    void handleEmailVerification_shouldCallEmailService_whenEventReceived() {
        kafkaTemplate.send(
            Topics.EMAIL_VERIFICATION,
            new EmailVerificationEvent("user@example.com", "test-token")
        );

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() ->
                verify(emailService)
                    .sendVerificationEmail("user@example.com", "test-token")
            );
    }

    @Test
    void handleEmailVerification_shouldRetryAndSendToDlt_whenExceptionOccurs() {
        doThrow(new RuntimeException("fail"))
            .when(emailService)
            .sendVerificationEmail(anyString(), anyString());

        kafkaTemplate.send(
            Topics.EMAIL_VERIFICATION,
            new EmailVerificationEvent("fail@gmail.com", "token")
        );

        await()
            .atMost(Duration.ofSeconds(40))
            .untilAsserted(() -> {

                verify(emailService, atLeast(4))
                    .sendVerificationEmail(anyString(), anyString());

                verify(emailConsumer)
                    .handleDlt(any(EmailVerificationEvent.class));
            });
    }

    @Test
    void handleEmailVerification_shouldNotRetry_whenIllegalArgumentException() {
        doThrow(new IllegalArgumentException("bad input"))
            .when(emailService)
            .sendVerificationEmail(anyString(), anyString());

        kafkaTemplate.send(
            Topics.EMAIL_VERIFICATION,
            new EmailVerificationEvent("bad@gmail.com", "token")
        );

        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {

                // only 1 call
                verify(emailService, times(1))
                    .sendVerificationEmail(anyString(), anyString());

                // sent to DLT
                verify(emailConsumer)
                    .handleDlt(any());
            });
    }
}