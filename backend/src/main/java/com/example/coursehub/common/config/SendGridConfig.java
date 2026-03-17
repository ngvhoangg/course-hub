package com.example.coursehub.common.config;

import com.sendgrid.SendGrid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SendGridConfig {

    private final SendGridProperties sendGridProperties;

    public SendGridConfig(SendGridProperties sendGridProperties) {
        this.sendGridProperties = sendGridProperties;
    }

    @Bean
    public SendGrid sendGrid() {
        return new SendGrid(sendGridProperties.getApiKey());
    }
}