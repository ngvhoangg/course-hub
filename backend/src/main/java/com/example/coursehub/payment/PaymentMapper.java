package com.example.coursehub.payment;

import com.example.coursehub.payment.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponse toUserPaymentResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getCourse().getTitle(),
            payment.getAmount(),
            payment.getProvider(),
            payment.getStatus(),
            payment.getCreatedAt()
        );
    }
}
