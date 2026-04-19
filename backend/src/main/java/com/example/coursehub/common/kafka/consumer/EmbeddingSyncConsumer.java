package com.example.coursehub.common.kafka.consumer;

import com.example.coursehub.ai.client.AIClient;
import com.example.coursehub.ai.embedding.EntityEmbedding;
import com.example.coursehub.ai.embedding.EntityEmbeddingRepository;
import com.example.coursehub.ai.embedding.EntityType;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.common.kafka.ConsumerGroups;
import com.example.coursehub.common.kafka.Topics;
import com.example.coursehub.common.kafka.event.EntityAction;
import com.example.coursehub.common.kafka.event.EntitySyncEvent;
import com.example.coursehub.course.Course;
import com.example.coursehub.course.CourseRepository;
import com.example.coursehub.lesson.Lesson;
import com.example.coursehub.lesson.LessonRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingSyncConsumer {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EntityEmbeddingRepository embeddingRepository;
    private final AIClient aiClient;
    private final ObjectMapper objectMapper;

    public EmbeddingSyncConsumer(CourseRepository courseRepository, LessonRepository lessonRepository, EntityEmbeddingRepository embeddingRepository, AIClient aiClient, ObjectMapper objectMapper) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.embeddingRepository = embeddingRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 3000, multiplier = 2.0),
        exclude = {
            NullPointerException.class,
            IllegalArgumentException.class
        },
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        retryTopicSuffix = "-retry",
        dltTopicSuffix = "-dlt"
    )
    @Transactional
    @KafkaListener(topics = Topics.ENTITY_SYNC, groupId = ConsumerGroups.EMBEDDING_SYNC_GROUP)
    public void consume(EntitySyncEvent event) throws JsonProcessingException {
        log.info("Processing event: {} ID: {}, Action: {}",
            event.entityType(), event.entityId(), event.action());

        // Ordering Check
        Optional<EntityEmbedding> existing = embeddingRepository.findByEntityTypeAndEntityId(event.entityType(), event.entityId());

        if (existing.isPresent()) {
            long lastUpdated = existing.get().getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (event.occurredAt() <= lastUpdated) {
                return;
            }
        }

        // Entity Action
        if (event.action() == EntityAction.DELETE) {
            handleDelete(event);
        } else if (event.action() == EntityAction.UPSERT) {
            handleUpsert(event);
        }
    }

    private void handleDelete(EntitySyncEvent event) {
        embeddingRepository.deleteByEntityTypeAndEntityId(event.entityType(), event.entityId());
        log.info("Deleted vector successfully for {} ID: {}", event.entityType(), event.entityId());
    }

    private void handleUpsert(EntitySyncEvent event) throws JsonProcessingException {
        if (event.entityType() == EntityType.COURSE) {
            handleCourseUpsert(event);
        } else if (event.entityType() == EntityType.LESSON){
            handleLessonUpsert(event);
        }
    }

    private void handleCourseUpsert(EntitySyncEvent event) throws JsonProcessingException {
        String metaJson = objectMapper.writeValueAsString(
            Optional.ofNullable(event.metadata()).orElse(Map.of())
        );

        if (event.reEmbed()) {
            // Update content + AI generate new Vector
            Course course = courseRepository.findById(event.entityId())
                .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

            String content = "Title: " + course.getTitle() + "\nDescription: " + course.getDescription();
            List<Double> vector = aiClient.getEmbedding(content);

            embeddingRepository.upsert(
                EntityType.COURSE.name(),
                course.getId(),
                content,
                vector.toString(),
                metaJson
            );
            log.info("Full Re-embed successfully for Course ID: {}", course.getId());
        } else {
            // Update metadata (price, status,...)
            embeddingRepository.patchMetadata(
                EntityType.COURSE.name(),
                event.entityId(),
                metaJson
            );
            log.info("Patch Metadata successfully for Course ID: {}", event.entityId());
        }
    }

    private void handleLessonUpsert(EntitySyncEvent event) throws JsonProcessingException {
        Lesson lesson = lessonRepository.findByIdWithCourse(event.entityId())
            .orElseThrow(() -> new UserError(ErrorCode.LESSON_NOT_FOUND));

        String metaJson = objectMapper.writeValueAsString(
            Optional.ofNullable(event.metadata()).orElse(Map.of())
        );

        if (event.reEmbed()) {
            String content = "Title: " + lesson.getTitle() + "\nContent: " + lesson.getContent();

            List<Double> vector = aiClient.getEmbedding(content);

            embeddingRepository.upsert(
                EntityType.LESSON.name(),
                lesson.getId(),
                content,
                vector.toString(),
                metaJson
            );

            log.info("Full Re-embed successfully for Lesson ID: {}", lesson.getId());
        } else {
            embeddingRepository.patchMetadata(
                EntityType.LESSON.name(),
                event.entityId(),
                metaJson
            );
            log.info("Patch Metadata successfully for Lesson ID: {}", event.entityId());
        }
    }

    @DltHandler
    public void handleDlt(EntitySyncEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("AI Sync failed permanently for {} ID: {} in topic: {}",
            event.entityType(), event.entityId(), topic);
    }
}
