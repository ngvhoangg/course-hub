package com.example.coursehub.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sendgrid")
public class SendGridProperties {
    private String apiKey;
    private String fromEmail;
    private String verificationUrl;
}