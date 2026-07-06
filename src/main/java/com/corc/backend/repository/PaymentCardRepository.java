package com.corc.backend.repository;

import com.corc.backend.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
    List<PaymentCard> findByUserIdOrderByCreatedAtDesc(Long userId);
}
