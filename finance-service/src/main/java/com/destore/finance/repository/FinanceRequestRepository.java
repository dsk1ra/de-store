package com.destore.finance.repository;

import com.destore.finance.entity.FinanceRequest;
import com.destore.finance.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinanceRequestRepository extends JpaRepository<FinanceRequest, Long> {
    Optional<FinanceRequest> findByRequestId(String requestId);
    List<FinanceRequest> findByStatus(RequestStatus status);
    List<FinanceRequest> findByCustomerId(String customerId);
}
