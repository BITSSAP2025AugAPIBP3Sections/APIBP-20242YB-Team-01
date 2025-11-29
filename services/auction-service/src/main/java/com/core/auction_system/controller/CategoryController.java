package com.core.auction_system.controller;

import com.core.auction_system.model.Category;
import com.core.auction_system.service.CategoryService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<?> getCategoryById(@PathVariable Integer id) {
        logger.debug("GET /api/categories/v1/{} called", id);
        return categoryService.getCategoryById(id)
                .map(c -> {
                    logger.info("Category found with id {}", id);
                    return ResponseEntity.ok((Object) c);
                })
                .orElseGet(() -> {
                    logger.warn("Category not found for id {}", id);
                    return ResponseEntity.status(404).body(Map.of(
                        "errorCode", 404,
                        "errorMessage", "Category not found"
                    ));
                });
    }

    /**
     * POST /api/categories/v1
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        logger.debug("POST /api/categories/v1 called with category: {}", category);
        try {
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", 400,
                    "errorMessage", "Category name is required"
                ));
            }
            Category saved = categoryService.createCategory(category);
            logger.info("Category created with id {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Error creating category: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "errorCode", 500,
                "errorMessage", "Failed to create category"
            ));
        }
    }

    /**
     * PUT /api/categories/v1/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        logger.debug("PUT /api/categories/v1/{} called with data: {}", id, category);
        try {
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", 400,
                    "errorMessage", "Category name is required"
                ));
            }
            Category updated = categoryService.updateCategory(id, category);
            if (updated == null) {
                logger.warn("Category update failed: id {} not found", id);
                return ResponseEntity.status(404).body(Map.of(
                    "errorCode", 404,
                    "errorMessage", "Category not found"
                ));
            }
            logger.info("Category updated with id {}", id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Error updating category with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "errorCode", 500,
                "errorMessage", "Failed to update category"
            ));
        }
    }

    /**
     * DELETE /api/categories/v1/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Integer id) {
        logger.debug("DELETE /api/categories/v1/{} called", id);
        try {
            // Check if category exists before deletion
            if (categoryService.getCategoryById(id).isEmpty()) {
                logger.warn("Category deletion failed: id {} not found", id);
                return ResponseEntity.status(404).body(Map.of(
                    "errorCode", 404,
                    "errorMessage", "Category not found"
                ));
            }
            categoryService.deleteCategory(id);
            logger.info("Category deleted with id {}", id);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Category deleted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error deleting category with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "errorCode", 500,
                "errorMessage", "Failed to delete category"
            ));
        }
    }
}
