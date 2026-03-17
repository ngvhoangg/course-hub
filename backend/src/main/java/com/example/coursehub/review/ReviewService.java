package com.example.coursehub.review;

import com.example.coursehub.review.dto.ReviewResponse;

import java.util.List;

public interface ReviewService {
    void createReview(Long userId, Long courseId, Integer rating, String comment);
    List<ReviewResponse> getCourseReviews(Long courseId);
}
