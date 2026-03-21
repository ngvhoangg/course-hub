package com.example.coursehub.common.email;

import com.example.coursehub.common.config.SendGridProperties;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.SystemError;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class EmailService {
    private final SendGrid sendGrid;
    private final SendGridProperties sendGridProperties;

    public EmailService(SendGrid sendGrid, SendGridProperties sendGridProperties) {
        this.sendGrid = sendGrid;
        this.sendGridProperties = sendGridProperties;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("toEmail must not be null or empty");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be null or empty");
        }
        if (sendGridProperties.getFromEmail() == null) {
            throw new IllegalStateException("SendGrid fromEmail is not configured");
        }
        if (sendGridProperties.getVerificationUrl() == null) {
            throw new IllegalStateException("Verification URL is not configured");
        }

        try {
            Email from = new Email(sendGridProperties.getFromEmail());
            Email to = new Email(toEmail);

            String subject = "Verify your Email";

            String verificationLink =
                sendGridProperties.getVerificationUrl() + "?token=" + token;

            Content content = new Content(
                "text/html",
                loadTemplate(verificationLink)
            );

            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            int statusCode = response.getStatusCode();

            if (statusCode != 202) {
                log.error(
                    "SendGrid failed. Status: {}, Body: {}",
                    statusCode,
                    response.getBody()
                );
                throw new SystemError(ErrorCode.EMAIL_SEND_FAILED);
            }
            log.info("Verification email sent to {} successfully", toEmail);
        } catch (IOException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            throw new SystemError(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String loadTemplate(String verificationLink) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/verification-email.html");
            String template = new String(resource.getInputStream().readAllBytes());
            return template.replace("{{verificationLink}}", verificationLink);
        } catch (IOException e) {
            log.error("Failed to load email template: {}", e.getMessage());
            throw new SystemError(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
