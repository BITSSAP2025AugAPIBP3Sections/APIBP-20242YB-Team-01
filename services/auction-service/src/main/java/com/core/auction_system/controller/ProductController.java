package com.core.auction_system.controller;

import com.core.auction_system.model.Product;
import com.core.auction_system.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/products/v1")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    /**
     * GET /api/products/v1
     */
    @GetMapping
    public List<Product> getAllProducts() {
        log.debug("GET /api/products/v1 called");
        List<Product> products = productService.getAllProducts();
        log.info("Fetched {} products", products.size());
        return products;
    }

    /**
     * GET /api/products/v1/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        log.debug("GET /api/products/v1/{} called", id);
        return productService.getProductById(id)
                .map(p -> {
                    log.info("Product found with id {}", id);
                    return ResponseEntity.ok(p);
                })
                .orElseGet(() -> {
                    log.warn("Product not found for id {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * POST /api/products/v1
     */
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        log.debug("POST /api/products/v1 called with product: {}", product);
        try {
            Product saved = productService.createProduct(product);
            log.info("Product created with id {}", saved.getId());
            return saved;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid product create request: {}", e.getMessage());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * PUT /api/products/v1/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Integer id, @RequestBody Product product) {
        log.debug("PUT /api/products/v1/{} called with data: {}", id, product);
        try {
            Product updated = productService.updateProduct(id, product);
            if (updated == null) {
                log.warn("Product update failed: id {} not found", id);
                return ResponseEntity.notFound().build();
            }
            log.info("Product updated with id {}", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid product update request for id {}: {}", id, e.getMessage());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * DELETE /api/products/v1/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        log.debug("DELETE /api/products/v1/{} called", id);
        productService.deleteProduct(id);
        log.info("Product deleted with id {}", id);
        return ResponseEntity.noContent().build();
    }
}
