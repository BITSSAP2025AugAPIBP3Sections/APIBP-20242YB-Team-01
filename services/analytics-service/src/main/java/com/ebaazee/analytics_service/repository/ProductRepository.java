package com.ebaazee.analytics_service.repository;

import com.ebaazee.analytics_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}
