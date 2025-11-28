package com.destore.inventory.repository;

import com.destore.inventory.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    
    Optional<Store> findByStoreId(String storeId);
    
    boolean existsByStoreId(String storeId);
    
    List<Store> findByActiveTrue();
    
    List<Store> findByCity(String city);
    
    List<Store> findByRegion(String region);
    
    List<Store> findByCountry(String country);
}
