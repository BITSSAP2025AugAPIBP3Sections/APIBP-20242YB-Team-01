package com.ebaazee.analytics_service.repository;

import com.ebaazee.analytics_service.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
