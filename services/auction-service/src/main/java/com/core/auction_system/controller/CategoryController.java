package com.core.auction_system.controller;

import com.core.auction_system.model.Category;
import com.core.auction_system.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/categories/v1")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    /**
     * GET /api/categories/v1
     */
    @GetMapping
    public List<Category> getAllCategories() {
        logger.debug("GET /api/categories/v1 called");
        List<Category> categories = categoryService.getAllCategories();
        logger.info("Fetched {} categories", categories.size());
        return categories;
    }

    /**
     * GET /api/categories/v1/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Integer id) {
        logger.debug("GET /api/categories/v1/{} called", id);
        return categoryService.getCategoryById(id)
                .map(c -> {
                    logger.info("Category found with id {}", id);
                    return ResponseEntity.ok(c);
                })
                .orElseGet(() -> {
                    logger.warn("Category not found for id {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * POST /api/categories/v1
     */
    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        logger.debug("POST /api/categories/v1 called with category: {}", category);
        Category saved = categoryService.createCategory(category);
        logger.info("Category created with id {}", saved.getId());
        return saved;
    }

    /**
     * PUT /api/categories/v1/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        logger.debug("PUT /api/categories/v1/{} called with data: {}", id, category);
        Category updated = categoryService.updateCategory(id, category);
        if (updated == null) {
            logger.warn("Category update failed: id {} not found", id);
            return ResponseEntity.notFound().build();
        }
        logger.info("Category updated with id {}", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/categories/v1/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        logger.debug("DELETE /api/categories/v1/{} called", id);
        categoryService.deleteCategory(id);
        logger.info("Category deleted with id {}", id);
        return ResponseEntity.noContent().build();
    }
}
