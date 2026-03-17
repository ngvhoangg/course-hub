package com.example.coursehub.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p JOIN FETCH p.course WHERE p.user.id = :userId")
    List<Payment> findByUserIdWithCourse(@Param("userId") Long userId);
}

