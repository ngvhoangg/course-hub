package com.example.coursehub.tasks;

import com.example.coursehub.ai.embedding.EmbeddingService;
import com.example.coursehub.course.Course;
import com.example.coursehub.course.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class InitialVectorSyncTask implements CommandLineRunner {
    private final CourseRepository courseRepository;
    private final EmbeddingService embeddingService;

    public InitialVectorSyncTask(CourseRepository courseRepository, EmbeddingService embeddingService) {
        this.courseRepository = courseRepository;
        this.embeddingService = embeddingService;
    }

    @Override
    public void run(String... args) {
        List<Course> courses = courseRepository.findCoursesWithoutEmbedding();

        log.info("Found {} courses without embedding", courses.size());

        for (Course c : courses) {
            String content = c.getTitle() + " " + c.getDescription();
            Map<String, Object> meta = Map.of("price", c.getPrice());
            embeddingService.sync("COURSE", c.getId(), content, meta);
        }

        log.info("Sync successfully!");
    }
}
