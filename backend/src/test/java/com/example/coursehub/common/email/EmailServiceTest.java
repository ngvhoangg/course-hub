package com.example.coursehub.common.email;

import com.example.coursehub.common.config.SendGridProperties;
import com.example.coursehub.common.exception.SystemError;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private SendGrid sendGrid;
    @Mock private SendGridProperties sendGridProperties;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        lenient().when(sendGridProperties.getFromEmail()).thenReturn("no-reply@coursehub.com");
        lenient().when(sendGridProperties.getVerificationUrl()).thenReturn("http://localhost:8081/api/auth/verify");
    }

    @Test
    void sendVerificationEmail_shouldCallSendGrid_whenInputsValid() throws Exception {
        Response response = new Response();
        response.setStatusCode(202);
        when(sendGrid.api(any(Request.class))).thenReturn(response);

        emailService.sendVerificationEmail("user@example.com", "valid-token");

        verify(sendGrid).api(any(Request.class));
    }

    @Test
    void sendVerificationEmail_shouldThrowSystemError_whenStatusCodeNot202() throws Exception {
        Response response = new Response();
        response.setStatusCode(400);
        response.setBody("Bad Request");
        when(sendGrid.api(any(Request.class))).thenReturn(response);

        assertThatThrownBy(() -> emailService.sendVerificationEmail("user@example.com", "token"))
            .isInstanceOf(SystemError.class);
    }

    @Test
    void sendVerificationEmail_shouldThrowSystemError_whenIOExceptionOccurs() throws Exception {
        when(sendGrid.api(any(Request.class))).thenThrow(new IOException("Network error"));

        assertThatThrownBy(() -> emailService.sendVerificationEmail("user@example.com", "token"))
            .isInstanceOf(SystemError.class);
    }

    @Test
    void sendVerificationEmail_shouldThrow_whenEmailIsNull() {
        assertThatThrownBy(() -> emailService.sendVerificationEmail(null, "token"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("toEmail");
    }

    @Test
    void sendVerificationEmail_shouldThrow_whenEmailIsBlank() {
        assertThatThrownBy(() -> emailService.sendVerificationEmail("  ", "token"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("toEmail");
    }

    @Test
    void sendVerificationEmail_shouldThrow_whenTokenIsNull() {
        assertThatThrownBy(() -> emailService.sendVerificationEmail("user@example.com", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("token");
    }

    @Test
    void sendVerificationEmail_shouldThrow_whenTokenIsBlank() {
        assertThatThrownBy(() -> emailService.sendVerificationEmail("user@example.com", "  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("token");
    }

    @Test
    void sendVerificationEmail_shouldThrow_whenFromEmailNotConfigured() {
        when(sendGridProperties.getFromEmail()).thenReturn(null);

        assertThatThrownBy(() -> emailService.sendVerificationEmail("user@example.com", "token"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("fromEmail");
    }

    @Test
    void sendVerificationEmail_shouldThrow_whenVerificationUrlNotConfigured() {
        when(sendGridProperties.getVerificationUrl()).thenReturn(null);

        assertThatThrownBy(() -> emailService.sendVerificationEmail("user@example.com", "token"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Verification URL");
    }
}