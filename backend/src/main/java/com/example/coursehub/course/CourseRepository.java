package com.example.coursehub.course;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("""
    SELECT c FROM Course c
    LEFT JOIN FETCH c.instructor
    LEFT JOIN FETCH c.categories
    LEFT JOIN FETCH c.lessons
    WHERE c.id = :id
    """)
    Optional<Course> findByIdWithDetails(@Param("id") Long id);

    @Query(value = """
    SELECT c.* FROM courses c, plainto_tsquery('english', :query) q
    WHERE c.search_vector @@ q
    ORDER BY ts_rank(c.search_vector, q) DESC
    """,
        countQuery = "SELECT count(*) FROM courses WHERE search_vector @@ plainto_tsquery('english', :query)",
        nativeQuery = true)
    Page<Course> searchByKeyword(@Param("query") String query, Pageable pageable);

    @Query(value = """
        SELECT c.*
        FROM courses c
        LEFT JOIN entity_embeddings e
          ON e.entity_type = 'COURSE'
         AND e.entity_id = c.id
        WHERE e.id IS NULL
    """, nativeQuery = true)
    List<Course> findCoursesWithoutEmbedding();
}
