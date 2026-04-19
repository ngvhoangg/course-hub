package com.example.coursehub.tasks;

import com.example.coursehub.ai.embedding.EmbeddingService;
import com.example.coursehub.ai.embedding.EntityType;
import com.example.coursehub.category.Category;
import com.example.coursehub.course.Course;
import com.example.coursehub.course.CourseRepository;
import com.example.coursehub.lesson.Lesson;
import com.example.coursehub.lesson.LessonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class InitialVectorSyncTask implements CommandLineRunner {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EmbeddingService embeddingService;

    public InitialVectorSyncTask(CourseRepository courseRepository, LessonRepository lessonRepository, EmbeddingService embeddingService) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.embeddingService = embeddingService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // ===== COURSE =====
        List<Course> courses = courseRepository.findCoursesWithoutEmbedding();
        log.info("Found {} courses without embedding", courses.size());

        for (Course c : courses) {
            String content = "Title: " + c.getTitle() + "\nDescription: " + c.getDescription();

            Map<String, Object> meta = new HashMap<>();
            if (c.getPrice() != null) {
                meta.put("price", c.getPrice());
            }
            if (c.getCategories() != null && !c.getCategories().isEmpty()) {
                meta.put("category_ids",
                    c.getCategories().stream().map(Category::getId).toList());
            }

            embeddingService.sync(EntityType.COURSE.name(), c.getId(), content, meta);
        }

        // ===== LESSON =====
        List<Lesson> lessons = lessonRepository.findLessonsWithoutEmbedding();
        log.info("Found {} lessons without embedding", lessons.size());

        for (Lesson l : lessons) {
            String content = "Title: " + l.getTitle() + "\nContent: " + l.getContent();

            Map<String, Object> meta = Map.of(
                "course_id", l.getCourse().getId(),
                "order_index", l.getOrderIndex()
            );

            embeddingService.sync(EntityType.LESSON.name(), l.getId(), content, meta);
        }

        log.info("Backfill embedding completed!");
    }
}
