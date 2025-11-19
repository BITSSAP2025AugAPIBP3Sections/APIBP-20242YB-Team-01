package com.core.auction_system.repository;

import com.core.auction_system.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory(String category);

    // Property is 'sellerId' on Product, use findBySellerId for Spring Data parsing
    List<Product> findBySellerId(Integer sellerId);

    List<Product> findByEndTimeBeforeAndFrozenFalse(LocalDateTime now);
}
