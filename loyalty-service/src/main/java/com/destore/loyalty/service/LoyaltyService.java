package com.destore.loyalty.service;

import com.destore.loyalty.dto.CustomerRequest;
import com.destore.loyalty.dto.PurchaseRequest;
import com.destore.loyalty.entity.Customer;
import com.destore.loyalty.entity.LoyaltyTier;
import com.destore.loyalty.entity.PurchaseHistory;
import com.destore.loyalty.repository.CustomerRepository;
import com.destore.loyalty.repository.PurchaseHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {
    
    private final CustomerRepository customerRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;
    
    @Value("${loyalty.points.per-dollar:10}")
    private int pointsPerDollar;
    
    @Value("${loyalty.points.silver-threshold:1000}")
    private int silverThreshold;
    
    @Value("${loyalty.points.gold-threshold:5000}")
    private int goldThreshold;
    
    @Transactional
    public Customer registerCustomer(CustomerRequest request) {
        if (customerRepository.existsByCustomerId(request.getCustomerId())) {
            throw new com.destore.exception.DuplicateResourceException("Customer", request.getCustomerId());
        }
        
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new com.destore.exception.DuplicateResourceException("Customer email", request.getEmail());
        }
        
        Customer customer = Customer.builder()
                .customerId(request.getCustomerId())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .loyaltyPoints(0)
                .loyaltyTier(LoyaltyTier.BRONZE)
                .totalSpent(BigDecimal.ZERO)
                .purchaseCount(0)
                .active(true)
                .build();
        
        Customer saved = customerRepository.save(customer);
        log.info("Registered new customer: {} with tier: {}", saved.getCustomerId(), saved.getLoyaltyTier());
        return saved;
    }
    
    public Customer getCustomer(String customerId) {
        return customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new com.destore.exception.ResourceNotFoundException("Customer", customerId));
    }
    
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    public List<Customer> getActiveCustomers() {
        return customerRepository.findByActiveTrue();
    }
    
    public List<Customer> getRegularCustomers() {
        return customerRepository.findByPurchaseCountGreaterThanEqual(3);
    }
    
    public List<Customer> getCustomersByTier(LoyaltyTier tier) {
        return customerRepository.findByLoyaltyTier(tier);
    }
    
    @Transactional
    public Customer recordPurchase(PurchaseRequest request) {
        Customer customer = getCustomer(request.getCustomerId());
        
        // Calculate points earned (10 points per dollar)
        int pointsEarned = request.getAmount().intValue() * pointsPerDollar;
        
        // Update customer
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsEarned);
        customer.setTotalSpent(customer.getTotalSpent().add(request.getAmount()));
        customer.setPurchaseCount(customer.getPurchaseCount() + 1);
        customer.setLastPurchaseDate(LocalDateTime.now());
        
        // Update tier based on points
        updateCustomerTier(customer);
        
        // Save purchase history
        PurchaseHistory history = PurchaseHistory.builder()
                .customerId(request.getCustomerId())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .pointsEarned(pointsEarned)
                .tierAtPurchase(customer.getLoyaltyTier())
                .items(request.getItems())
                .build();
        
        purchaseHistoryRepository.save(history);
        
        Customer updated = customerRepository.save(customer);
        log.info("Recorded purchase for customer: {}, points earned: {}, new tier: {}", 
                customer.getCustomerId(), pointsEarned, customer.getLoyaltyTier());
        
        return updated;
    }
    
    @Transactional
    public Customer redeemPoints(String customerId, Integer points) {
        Customer customer = getCustomer(customerId);
        
        if (customer.getLoyaltyPoints() < points) {
            throw new com.destore.exception.InvalidRequestException(
                    "Insufficient points. Available: " + customer.getLoyaltyPoints() + ", Required: " + points);
        }
        
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - points);
        updateCustomerTier(customer);
        
        Customer updated = customerRepository.save(customer);
        log.info("Redeemed {} points for customer: {}", points, customerId);
        return updated;
    }
    
    @Transactional
    public Customer updateCustomer(String customerId, CustomerRequest request) {
        Customer customer = getCustomer(customerId);
        
        if (request.getName() != null) {
            customer.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.getEmail())) {
                throw new com.destore.exception.DuplicateResourceException("Customer email", request.getEmail());
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        
        Customer updated = customerRepository.save(customer);
        log.info("Updated customer: {}", customerId);
        return updated;
    }
    
    @Transactional
    public void deactivateCustomer(String customerId) {
        Customer customer = getCustomer(customerId);
        customer.setActive(false);
        customerRepository.save(customer);
        log.info("Deactivated customer: {}", customerId);
    }
    
    public List<PurchaseHistory> getCustomerPurchaseHistory(String customerId) {
        return purchaseHistoryRepository.findByCustomerIdOrderByPurchaseDateDesc(customerId);
    }
    
    private void updateCustomerTier(Customer customer) {
        LoyaltyTier oldTier = customer.getLoyaltyTier();
        LoyaltyTier newTier;
        
        if (customer.getLoyaltyPoints() >= goldThreshold) {
            newTier = LoyaltyTier.GOLD;
        } else if (customer.getLoyaltyPoints() >= silverThreshold) {
            newTier = LoyaltyTier.SILVER;
        } else {
            newTier = LoyaltyTier.BRONZE;
        }
        
        if (oldTier != newTier) {
            customer.setLoyaltyTier(newTier);
            log.info("Customer {} tier upgraded from {} to {}", customer.getCustomerId(), oldTier, newTier);
        }
    }
    
    public int calculateLoyaltyDiscount(String customerId) {
        Customer customer = getCustomer(customerId);
        return customer.getDiscountPercentage();
    }
}
