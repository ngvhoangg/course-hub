package com.example.coursehub.common.kafka.consumer;

import com.example.coursehub.common.email.EmailService;
import com.example.coursehub.common.kafka.event.EmailVerificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailConsumer emailConsumer;

    @Test
    void shouldCallService_WhenEventReceived() {
        // Arrange
        EmailVerificationEvent event = new EmailVerificationEvent("test@gmail.com", "token123");

        // Act
        emailConsumer.handleEmailVerification(event);

        // Assert
        verify(emailService, times(1)).sendVerificationEmail("test@gmail.com", "token123");
    }
}