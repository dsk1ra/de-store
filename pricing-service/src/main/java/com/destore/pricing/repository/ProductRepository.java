package com.destore.pricing.repository;

import com.destore.pricing.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductCode(String productCode);
    boolean existsByProductCode(String productCode);
    
    /**
     * Search products by name or code using database query instead of in-memory filtering.
     * This significantly improves performance for large product catalogs.
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Product> searchByNameOrCode(@Param("term") String term);
}
