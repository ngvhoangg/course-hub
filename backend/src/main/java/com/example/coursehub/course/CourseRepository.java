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

    // keyword search
    @Query(value = "SELECT * FROM courses WHERE search_vector @@ plainto_tsquery('english', :query)",
        nativeQuery = true)
    Page<Course> searchByKeyword(@Param("query") String query, Pageable pageable);

    // semantic search
    @Query(value = """
        SELECT c.* FROM courses c
        JOIN entity_embeddings e ON c.id = e.entity_id
        WHERE e.entity_type = 'COURSE'
        ORDER BY e.embedding <=> CAST(:vector AS vector)
        """,
        countQuery = "SELECT count(*) FROM entity_embeddings WHERE entity_type = 'COURSE'",
        nativeQuery = true)
    Page<Course> searchSemantic(@Param("vector") String vector, Pageable pageable);

    // hybrid search
    @Query(value = """
        WITH keyword_search AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY ts_rank_cd(search_vector, plainto_tsquery('english', :query)) DESC) as rank
            FROM courses
            WHERE search_vector @@ plainto_tsquery('english', :query)
            LIMIT 100
        ),
        semantic_search AS (
            SELECT entity_id AS id, ROW_NUMBER() OVER (ORDER BY embedding <=> CAST(:vector AS vector)) as rank
            FROM entity_embeddings
            WHERE entity_type = 'COURSE'
            LIMIT 100
        )
        SELECT c.* FROM courses c
        LEFT JOIN keyword_search k ON c.id = k.id
        LEFT JOIN semantic_search s ON c.id = s.id
        WHERE k.id IS NOT NULL OR s.id IS NOT NULL
        ORDER BY (COALESCE(1.0 / (60 + k.rank), 0.0) + COALESCE(1.0 / (60 + s.rank), 0.0)) DESC
        """,
        countQuery = """
            SELECT count(DISTINCT id) FROM (
                SELECT id FROM courses WHERE search_vector @@ plainto_tsquery('english', :query)
                UNION
                SELECT entity_id FROM entity_embeddings WHERE entity_type = 'COURSE'
            ) as combined
        """,
        nativeQuery = true)
    Page<Course> searchHybrid(@Param("query") String query, @Param("vector") String vector, Pageable pageable);

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
