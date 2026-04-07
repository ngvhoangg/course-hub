package com.example.coursehub.ai.vector;

import com.example.coursehub.course.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorSearchRepository extends JpaRepository<Course, Long> {
    @Query(value = """
        SELECT c.* FROM courses c
        JOIN entity_embeddings e ON c.id = e.entity_id
        WHERE e.entity_type = 'COURSE'
        ORDER BY e.embedding <=> CAST(:queryVector AS vector)
        """,
        countQuery = """
        SELECT count(*) FROM courses c
        JOIN entity_embeddings e ON c.id = e.entity_id
        WHERE e.entity_type = 'COURSE'
        """,
        nativeQuery = true)
    Page<Course> searchSimilarCourses(@Param("queryVector") String queryVector, Pageable pageable);
}
