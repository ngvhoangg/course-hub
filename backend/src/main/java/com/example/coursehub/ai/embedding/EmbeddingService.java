package com.example.coursehub.ai.embedding;

import com.example.coursehub.ai.chunking.LessonChunker;
import com.example.coursehub.ai.chunking.dto.ChunkResult;
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
    private final EntityEmbeddingRepository entityEmbeddingRepository;
    private final ObjectMapper objectMapper;
    private final LessonChunker lessonChunker;
    private final ChunkIdGenerator chunkIdGenerator;

    public EmbeddingService(AIClient aiClient, EntityEmbeddingRepository entityEmbeddingRepository, ObjectMapper objectMapper, LessonChunker lessonChunker, ChunkIdGenerator chunkIdGenerator) {
        this.aiClient = aiClient;
        this.entityEmbeddingRepository = entityEmbeddingRepository;
        this.objectMapper = objectMapper;
        this.lessonChunker = lessonChunker;
        this.chunkIdGenerator = chunkIdGenerator;
    }

    public void sync(String type, Long id, String content, Map<String, Object> metadata) {
        List<Double> vector = aiClient.getEmbedding(content);
        try {
            String metaJson = objectMapper.writeValueAsString(metadata);
            entityEmbeddingRepository.upsert(type, id, content, vector.toString(), metaJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialize metadata error", e);
        }
    }

    public void syncLessonChunks(Long lessonId, String content, Long courseId) {
        List<ChunkResult> chunks = lessonChunker.chunk(content);
        entityEmbeddingRepository.deleteLessonChunks(lessonId);

        for (ChunkResult chunk : chunks) {
            Long chunkId = chunkIdGenerator.generate(lessonId, chunk.chunkIndex());

            Map<String, Object> meta = Map.of(
                "lessonId", lessonId,
                "courseId", courseId,
                "chunkIndex", chunk.chunkIndex(),
                "totalChunks", chunks.size(),
                "tokenCount", chunk.tokenCount(),
                "startOffset", chunk.startOffset(),
                "endOffset", chunk.endOffset()
            );

            try {
                List<Double> vector = aiClient.getEmbedding(chunk.content());
                String metaJson = objectMapper.writeValueAsString(meta);
                entityEmbeddingRepository.upsert(
                    EntityType.LESSON_CHUNK.name(),
                    chunkId,
                    chunk.content(),
                    vector.toString(),
                    metaJson
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Serialize chunk metadata error for lessonId=" + lessonId, e);
            }
        }
    }
}
