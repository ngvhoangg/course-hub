package com.example.coursehub.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.course.id = :courseId")
    List<Review> findByCourseIdWithUser(@Param("courseId") Long courseId);
}
