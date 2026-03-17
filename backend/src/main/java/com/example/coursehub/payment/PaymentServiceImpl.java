package com.example.coursehub.payment;

import com.example.coursehub.course.Course;
import com.example.coursehub.user.User;
import com.example.coursehub.payment.dto.PaymentResponse;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import com.example.coursehub.course.CourseRepository;
import com.example.coursehub.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentMapper paymentMapper, UserRepository userRepository, CourseRepository courseRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public PaymentResponse createPayment(Long userId, Long courseId, String provider) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserError(ErrorCode.USER_NOT_FOUND));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new UserError(ErrorCode.COURSE_NOT_FOUND));

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setCourse(course);
        payment.setAmount(course.getPrice());
        payment.setProvider(provider);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
        return paymentMapper.toUserPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getUserPayments(Long id) {
        return paymentRepository.findByUserIdWithCourse(id)
            .stream()
            .map(paymentMapper::toUserPaymentResponse)
            .toList();
    }
}
