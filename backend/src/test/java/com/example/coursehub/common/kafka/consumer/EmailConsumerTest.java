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
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.kafka.listener.auto-startup=true"
})
@Testcontainers
@ActiveProfiles("test")
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
        EmailVerificationEvent event =
            new EmailVerificationEvent("user@example.com", "test-token");

        emailConsumer.handleEmailVerification(event);

        verify(emailService, times(1))
            .sendVerificationEmail("user@example.com", "test-token");
    }

    @Test
    void handleEmailVerification_shouldRetryAndSendToDlt_whenExceptionOccurs() throws Exception {
        doThrow(new RuntimeException("Simulated email failure"))
            .when(emailService)
            .sendVerificationEmail(anyString(), anyString());

        EmailVerificationEvent event = new EmailVerificationEvent("fail@gmail.com", "token");

        kafkaTemplate.send(Topics.EMAIL_VERIFICATION, event).get(10, TimeUnit.SECONDS);

        await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .untilAsserted(() -> {
                verify(emailService, atLeast(2))
                    .sendVerificationEmail(eq("fail@gmail.com"), anyString());

                verify(emailConsumer, atLeastOnce())
                    .handleDlt(any(EmailVerificationEvent.class));
            });
    }
}