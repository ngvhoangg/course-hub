package com.example.coursehub.lesson;

import com.example.coursehub.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson,Long> {
    List<Lesson> findByCourseId(Long courseId);

    @Query("SELECT l FROM Lesson l JOIN FETCH l.course WHERE l.id = :id")
    Optional<Lesson> findByIdWithCourse(@Param("id") Long id);

    @Query(value = """
        SELECT l.*
        FROM lessons l
        LEFT JOIN entity_embeddings e
          ON e.entity_type = 'LESSON'
          AND e.entity_id = l.id
        WHERE e.id IS NULL;
    """, nativeQuery = true)
    List<Lesson> findLessonsWithoutEmbedding();

    @Query(value = """
        SELECT l.id FROM lessons l
        WHERE NOT EXISTS (
            SELECT 1 FROM entity_embeddings e
            WHERE e.entity_type = 'LESSON_CHUNK'
              AND e.metadata->>'lessonId' = CAST(l.id AS text)
        )
    """, nativeQuery = true)
    List<Long> findLessonIdsWithoutChunks();
}
