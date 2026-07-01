package com.example.coursehub.common.kafka.consumer;

import com.example.coursehub.ai.chunking.LessonChunker;
import com.example.coursehub.ai.chunking.dto.ChunkResult;
import com.example.coursehub.ai.client.AIClient;
import com.example.coursehub.ai.embedding.ChunkIdGenerator;
import com.example.coursehub.ai.embedding.EntityEmbedding;
import com.example.coursehub.ai.embedding.EntityEmbeddingRepository;
import com.example.coursehub.ai.embedding.EntityType;
import com.example.coursehub.common.kafka.event.EntityAction;
import com.example.coursehub.common.kafka.event.EntitySyncEvent;
import com.example.coursehub.course.Course;
import com.example.coursehub.course.CourseRepository;
import com.example.coursehub.lesson.Lesson;
import com.example.coursehub.lesson.LessonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingSyncConsumerTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private EntityEmbeddingRepository embeddingRepository;

    @Mock
    private AIClient aiClient;

    @Mock
    private LessonChunker lessonChunker;

    @Mock
    private ChunkIdGenerator chunkIdGenerator;

    private EmbeddingSyncConsumer embeddingSyncConsumer;

    @BeforeEach
    void setUp() {
        embeddingSyncConsumer = new EmbeddingSyncConsumer(
            courseRepository,
            lessonRepository,
            embeddingRepository,
            aiClient,
            new ObjectMapper(),
            lessonChunker,
            chunkIdGenerator
        );
    }

    @Test
    void consume_shouldReembedLessonAndRegenerateChunksOnLessonUpsert() throws Exception {
        long lessonId = 15L;
        long courseId = 4L;

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Lesson title");
        lesson.setContent("Lesson content");
        lesson.setOrderIndex(3);

        Course course = new Course();
        course.setId(courseId);
        lesson.setCourse(course);

        when(embeddingRepository.findByEntityTypeAndEntityId(EntityType.LESSON, lessonId))
            .thenReturn(Optional.empty());
        when(lessonRepository.findByIdWithCourse(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonChunker.chunk("Lesson content")).thenReturn(List.of(
            new ChunkResult(0, "Chunk text", 3, 0, 10)
        ));
        when(chunkIdGenerator.generate(lessonId, 0)).thenReturn(998_877L);
        when(aiClient.getEmbedding("Title: Lesson title\nContent: Lesson content"))
            .thenReturn(List.of(1.0, 2.0));
        when(aiClient.getEmbedding("Chunk text"))
            .thenReturn(List.of(3.0, 4.0));

        EntitySyncEvent event = new EntitySyncEvent(
            lessonId,
            EntityType.LESSON,
            EntityAction.UPSERT,
            Map.of("order_index", 3),
            true,
            System.currentTimeMillis()
        );

        embeddingSyncConsumer.consume(event);

        verify(embeddingRepository, times(1)).deleteLessonChunks(lessonId);
        verify(embeddingRepository, times(1)).upsert(
            eq(EntityType.LESSON.name()),
            eq(lessonId),
            eq("Title: Lesson title\nContent: Lesson content"),
            eq("[1.0, 2.0]"),
            anyString()
        );
        verify(embeddingRepository, times(1)).upsert(
            eq(EntityType.LESSON_CHUNK.name()),
            eq(998_877L),
            eq("Chunk text"),
            eq("[3.0, 4.0]"),
            anyString()
        );
        verify(aiClient, times(1)).getEmbedding("Title: Lesson title\nContent: Lesson content");
        verify(aiClient, times(1)).getEmbedding("Chunk text");
    }

    @Test
    void consume_shouldDeleteLessonAndLessonChunksOnLessonDelete() throws Exception {
        long lessonId = 21L;

        EntityEmbedding existing = new EntityEmbedding();
        existing.setUpdatedAt(LocalDateTime.now().minusMinutes(5));

        when(embeddingRepository.findByEntityTypeAndEntityId(EntityType.LESSON, lessonId))
            .thenReturn(Optional.of(existing));

        EntitySyncEvent event = new EntitySyncEvent(
            lessonId,
            EntityType.LESSON,
            EntityAction.DELETE,
            Map.of(),
            false,
            System.currentTimeMillis()
        );

        embeddingSyncConsumer.consume(event);

        verify(embeddingRepository, times(1)).deleteByEntityTypeAndEntityId(EntityType.LESSON, lessonId);
        verify(embeddingRepository, times(1)).deleteLessonChunks(lessonId);
        verify(lessonRepository, never()).findByIdWithCourse(lessonId);
        verify(aiClient, never()).getEmbedding(anyString());
    }
}
