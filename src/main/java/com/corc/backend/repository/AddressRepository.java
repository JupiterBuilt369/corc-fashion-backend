package com.corc.backend.repository;

import com.corc.backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByCreatedAtDesc(Long userId);
}
