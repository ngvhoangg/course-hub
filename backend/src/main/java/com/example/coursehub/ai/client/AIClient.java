package com.example.coursehub.ai.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = true)
public class AIClient {
    private final RestClient restClient;

    public AIClient(@Value("${ai.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public List<Double> getEmbedding(String text) {
        Map<String, Object> response = restClient.post()
            .uri("/embed")
            .body(Map.of("text", text))
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        return (List<Double>) response.get("embedding");
    }
}
