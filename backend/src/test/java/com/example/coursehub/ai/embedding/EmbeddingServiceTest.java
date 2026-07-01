package com.example.coursehub.ai.embedding;

import com.example.coursehub.ai.chunking.LessonChunker;
import com.example.coursehub.ai.chunking.dto.ChunkResult;
import com.example.coursehub.ai.client.AIClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private AIClient aiClient;

    @Mock
    private LessonChunker lessonChunker;

    @Mock
    private EntityEmbeddingRepository entityEmbeddingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Long, StoredEmbedding> storedEmbeddings = new ConcurrentHashMap<>();
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(
            aiClient,
            entityEmbeddingRepository,
            objectMapper,
            lessonChunker,
            new ChunkIdGenerator()
        );

        storedEmbeddings.clear();

        doAnswer(invocation -> {
            String type = invocation.getArgument(0, String.class);
            Long id = invocation.getArgument(1, Long.class);
            String content = invocation.getArgument(2, String.class);
            String vector = invocation.getArgument(3, String.class);
            String metadata = invocation.getArgument(4, String.class);
            storedEmbeddings.put(id, new StoredEmbedding(type, content, vector, metadata));
            return null;
        }).when(entityEmbeddingRepository).upsert(anyString(), anyLong(), anyString(), anyString(), anyString());

        doAnswer(invocation -> {
                Long lessonId = invocation.getArgument(0, Long.class);
                storedEmbeddings.entrySet().removeIf(entry -> {
                    try {
                        JsonNode metadata = objectMapper.readTree(entry.getValue().metadata());
                        return EntityType.LESSON_CHUNK.name().equals(entry.getValue().type())
                            && metadata.has("lessonId")
                            && metadata.get("lessonId").asLong() == lessonId;
                    } catch (Exception e) {
                        return false;
                    }
                });
                return null;
            }).when(entityEmbeddingRepository).deleteLessonChunks(anyLong());
    }

    @Test
    void syncLessonChunks_shouldDeleteOldLessonChunksAndInsertNewChunks() throws Exception {
        long lessonId = 42L;
        long courseId = 9L;

        storedEmbeddings.put(9_999L, new StoredEmbedding(
            EntityType.LESSON_CHUNK.name(),
            "Old chunk",
            "[0.0, 0.0]",
            objectMapper.writeValueAsString(
                java.util.Map.of("lessonId", lessonId, "courseId", courseId, "chunkIndex", 0)
            )
        ));

        List<ChunkResult> chunks = List.of(
            new ChunkResult(0, "Alpha. Beta.", 4, 0, 12),
            new ChunkResult(1, "Beta. Gamma.", 4, 7, 19)
        );

        when(lessonChunker.chunk(anyString())).thenReturn(chunks);
        when(aiClient.getEmbedding("Alpha. Beta.")).thenReturn(List.of(0.1, 0.2));
        when(aiClient.getEmbedding("Beta. Gamma.")).thenReturn(List.of(0.3, 0.4));

        embeddingService.syncLessonChunks(lessonId, "Alpha. Beta. Gamma.", courseId);

        assertThat(storedEmbeddings).hasSize(2);
        assertThat(storedEmbeddings).doesNotContainKey(9_999L);
        assertThat(countLessonChunkRows(lessonId)).isEqualTo(2);

        assertThat(readMetadata(lessonId, 0).get("lessonId").asLong()).isEqualTo(lessonId);
        assertThat(readMetadata(lessonId, 0).get("courseId").asLong()).isEqualTo(courseId);
        assertThat(readMetadata(lessonId, 0).get("chunkIndex").asInt()).isEqualTo(0);
        assertThat(readMetadata(lessonId, 0).get("totalChunks").asInt()).isEqualTo(2);
        assertThat(readMetadata(lessonId, 0).get("startOffset").asInt()).isEqualTo(0);
        assertThat(readMetadata(lessonId, 0).get("endOffset").asInt()).isEqualTo(12);

        assertThat(readMetadata(lessonId, 1).get("chunkIndex").asInt()).isEqualTo(1);
        assertThat(readMetadata(lessonId, 1).get("totalChunks").asInt()).isEqualTo(2);
        assertThat(readMetadata(lessonId, 1).get("startOffset").asInt()).isEqualTo(7);
        assertThat(readMetadata(lessonId, 1).get("endOffset").asInt()).isEqualTo(19);

        verify(lessonChunker, times(1)).chunk("Alpha. Beta. Gamma.");
        verify(aiClient, times(1)).getEmbedding("Alpha. Beta.");
        verify(aiClient, times(1)).getEmbedding("Beta. Gamma.");
    }

    @Test
    void syncLessonChunks_shouldRemainIdempotentForTheSameLesson() {
        long lessonId = 77L;
        long courseId = 11L;

        List<ChunkResult> chunks = List.of(
            new ChunkResult(0, "One. Two.", 4, 0, 9),
            new ChunkResult(1, "Two. Three.", 4, 5, 16)
        );

        when(lessonChunker.chunk(anyString())).thenReturn(chunks);
        when(aiClient.getEmbedding("One. Two.")).thenReturn(List.of(0.1, 0.2));
        when(aiClient.getEmbedding("Two. Three.")).thenReturn(List.of(0.3, 0.4));

        embeddingService.syncLessonChunks(lessonId, "One. Two. Three.", courseId);
        long rowsAfterFirstSync = storedEmbeddings.size();

        embeddingService.syncLessonChunks(lessonId, "One. Two. Three.", courseId);

        assertThat(storedEmbeddings.size()).isEqualTo(rowsAfterFirstSync);
        assertThat(countLessonChunkRows(lessonId)).isEqualTo(2);
        verify(lessonChunker, times(2)).chunk("One. Two. Three.");
        verify(aiClient, times(2)).getEmbedding("One. Two.");
        verify(aiClient, times(2)).getEmbedding("Two. Three.");
    }

    private long countLessonChunkRows(long lessonId) {
        return storedEmbeddings.values().stream()
            .filter(embedding -> EntityType.LESSON_CHUNK.name().equals(embedding.type()))
            .filter(embedding -> {
                try {
                    JsonNode metadata = objectMapper.readTree(embedding.metadata());
                    return metadata.has("lessonId") && metadata.get("lessonId").asLong() == lessonId;
                } catch (Exception e) {
                    return false;
                }
            })
            .count();
    }

    private JsonNode readMetadata(long lessonId, int chunkIndex) throws Exception {
        for (StoredEmbedding embedding : storedEmbeddings.values()) {
            if (!EntityType.LESSON_CHUNK.name().equals(embedding.type())) {
                continue;
            }
            JsonNode metadata = objectMapper.readTree(embedding.metadata());
            if (metadata.has("lessonId") && metadata.get("lessonId").asLong() == lessonId
                && metadata.has("chunkIndex") && metadata.get("chunkIndex").asInt() == chunkIndex) {
                return metadata;
            }
        }
        throw new IllegalStateException("Metadata not found for lessonId=" + lessonId + ", chunkIndex=" + chunkIndex);
    }

    private record StoredEmbedding(String type, String content, String vector, String metadata) {
    }
}
