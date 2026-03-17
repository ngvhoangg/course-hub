package com.example.coursehub.payment;

import com.example.coursehub.payment.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {
    PaymentResponse createPayment(Long userId, Long courseId, String provider);
    List<PaymentResponse> getUserPayments(Long id);
}
