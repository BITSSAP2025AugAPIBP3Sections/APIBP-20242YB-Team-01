package com.core.auction_system.repository;

import com.core.auction_system.model.Product;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory(String category);

    // Property is 'sellerId' on Product, use findBySellerId for Spring Data parsing
    List<Product> findBySellerId(Integer sellerId);

    List<Product> findByEndTimeBeforeAndFrozenFalse(LocalDateTime now);
}
