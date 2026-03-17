package com.example.coursehub.course;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
