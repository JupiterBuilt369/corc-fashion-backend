package com.corc.backend.repository;

import com.corc.backend.entity.StockAdjustmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAdjustmentLogRepository extends JpaRepository<StockAdjustmentLog, Long> {
    List<StockAdjustmentLog> findByProductIdOrderByCreatedAtDesc(Long productId);
}
