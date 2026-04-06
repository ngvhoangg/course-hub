package com.example.coursehub.ai.embedding;

import com.example.coursehub.ai.client.AIClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingService {
    private final AIClient aiClient;
    private final EntityEmbeddingRepository entityEmbeddingrepository;
    private final ObjectMapper objectMapper;

    public EmbeddingService(AIClient aiClient, EntityEmbeddingRepository entityEmbeddingrepository, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.entityEmbeddingrepository = entityEmbeddingrepository;
        this.objectMapper = objectMapper;
    }

    public void sync(String type, Long id, String content, Map<String, Object> metadata) {
        List<Double> vector = aiClient.getEmbedding(content);
        try {
            String metaJson = objectMapper.writeValueAsString(metadata);
            entityEmbeddingrepository.upsert(type, id, content, vector.toString(), metaJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialize metadata error", e);
        }
    }
}
