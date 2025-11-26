package com.destore.loyalty.repository;

import com.destore.loyalty.entity.Customer;
import com.destore.loyalty.entity.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerId(String customerId);
    Optional<Customer> findByEmail(String email);
    boolean existsByCustomerId(String customerId);
    boolean existsByEmail(String email);
    List<Customer> findByLoyaltyTier(LoyaltyTier tier);
    List<Customer> findByActiveTrue();
    List<Customer> findByPurchaseCountGreaterThanEqual(Integer count);
}
