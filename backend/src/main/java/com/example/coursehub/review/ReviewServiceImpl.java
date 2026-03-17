package com.example.coursehub.review;

import com.example.coursehub.course.Course;
import com.example.coursehub.user.User;
import com.example.coursehub.review.dto.ReviewResponse;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.course.CourseRepository;
import com.example.coursehub.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(UserRepository userRepository, CourseRepository courseRepository, ReviewRepository reviewRepository, ReviewMapper reviewMapper) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional
    public void createReview(Long userId, Long courseId, Integer rating, String comment) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserError(ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

        if (reviewRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new UserError(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = new Review();
        review.setUser(user);
        review.setCourse(course);
        review.setRating(rating);
        review.setComment(comment);
        review.setStatus(ReviewStatus.VISIBLE);
        review.setCreated_at(LocalDateTime.now());

        reviewRepository.save(review);
    }

    @Override
    public List<ReviewResponse> getCourseReviews(Long courseId) {
        return reviewRepository.findByCourseIdWithUser(courseId)
            .stream()
            .map(reviewMapper::toReviewResponse)
            .toList();
    }
}
