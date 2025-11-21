package com.core.auction_system.controller;

import com.core.auction_system.model.Category;
import com.core.auction_system.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    // added
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public List<Category> getAllCategories() {
        logger.info("Inside getAllCategories()");
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Integer id) {
        logger.info("Inside getCategoryById() with id: {}", id);
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        logger.info("Inside createCategory()");
        return categoryService.createCategory(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        logger.info("Inside updateCategory() with id: {}", id);
        Category updated = categoryService.updateCategory(id, category);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        logger.info("Inside deleteCategory() with id: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
