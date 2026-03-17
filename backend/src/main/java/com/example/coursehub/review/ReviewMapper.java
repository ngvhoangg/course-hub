package com.example.coursehub.review;

import com.example.coursehub.review.dto.ReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewResponse toReviewResponse(Review review) {
        return new ReviewResponse(
            review.getRating(),
            review.getComment(),
            review.getUser().getFullName(),
            review.getCreated_at()
        );
    }
}
