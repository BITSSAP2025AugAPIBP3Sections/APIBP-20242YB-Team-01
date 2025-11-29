    // ...existing code...
package com.core.auction_system.controller;

import com.core.auction_system.dto.ProductCreateDTO;
import com.core.auction_system.model.Product;
import com.core.auction_system.service.ProductService;
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
@RequestMapping("/api/products/v1")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    /**
     * GET /api/products/v1/users/{userId}
     * Returns all products posted by a particular seller (userId)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<Product>> getProductsBySeller(@PathVariable Integer userId) {
        List<Product> products = productService.getProductsBySeller(userId);
        return ResponseEntity.ok(products);
    }

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
    public ResponseEntity<?> getProductById(@PathVariable Integer id) {
        log.debug("GET /api/products/v1/{} called", id);
        return productService.getProductById(id)
                .map(p -> {
                    log.info("Product found with id {}", id);
                    return ResponseEntity.ok((Object) p);
                })
                .orElseGet(() -> {
                    log.warn("Product not found for id {}", id);
                    return ResponseEntity.status(404).body(Map.of(
                        "errorCode", 404,
                        "errorMessage", "Product not found"
                    ));
                });
    }

    /**
     * GET /api/products/v1/category/{category}
     */
    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        log.debug("GET /api/products/v1/category/{} called", category);
        List<Product> products = productService.getProductsByCategory(category);
        log.info("Found {} products for category '{}'", products.size(), category);
        return products;
    }

    /**
     * POST /api/products/v1
     * Creates a new product. Accepts only: name, description, category, minBid, maxBid, endTime.
     * Seller ID is extracted from JWT token.
     * isFrozen and isSold are automatically set to false.
     * endTime must be on the hour (e.g., 12:00, 1:00, 2:00, not 12:30).
     */
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductCreateDTO productDto) {
        log.debug("POST /api/products/v1 called with DTO: {}", productDto);

        // Extract seller ID from JWT token
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            log.warn("Unauthenticated product creation attempt");
            return ResponseEntity.status(401).body(Map.of(
                    "errorCode", 401,
                    "errorMessage", "Authentication required"
            ));
        }

        // Extract seller ID from JWT token (stored in auth details)
        Integer sellerId = null;
        Object details = auth.getDetails();
        log.debug("Extracting seller id from JWT SecurityContext details: {}", details);
        if (details instanceof Integer) {
            sellerId = (Integer) details;
        } else if (details instanceof Number) {
            sellerId = ((Number) details).intValue();
        } else if (details != null) {
            try {
                sellerId = Integer.parseInt(details.toString());
            } catch (Exception e) {
                log.error("Failed to parse seller id from JWT: {}", details);
            }
        }

        if (sellerId == null) {
            log.error("Seller ID not found in JWT token");
            return ResponseEntity.status(401).body(Map.of(
                    "errorCode", 401,
                    "errorMessage", "Invalid token: seller ID missing"
            ));
        }

        String sellerEmail = auth.getName();
        log.info("Creating product for seller: {} (ID: {})", sellerEmail, sellerId);

        try {
            Product saved = productService.createProductFromDTO(productDto, sellerId);
            log.info("Product created with id {} by seller {}", saved.getId(), sellerId);

            // Build clean success response
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Product created successfully");
            response.put("id", saved.getId());
            response.put("name", saved.getName());
            response.put("description", saved.getDescription());
            response.put("category", saved.getCategory());
            response.put("minBid", saved.getMinBid());
            response.put("maxBid", saved.getMaxBid());
            response.put("endTime", saved.getEndTime());
            response.put("sellerId", saved.getSellerId());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid product create request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", 400,
                    "errorMessage", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error creating product", e);
            return ResponseEntity.status(500).body(Map.of(
                    "errorCode", 500,
                    "errorMessage", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/products/v1/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @RequestBody Product product) {
        log.debug("PUT /api/products/v1/{} called with data: {}", id, product);
        try {
            Product updated = productService.updateProduct(id, product);
            if (updated == null) {
                log.warn("Product update failed: id {} not found", id);
                return ResponseEntity.status(404).body(Map.of(
                    "errorCode", 404,
                    "errorMessage", "Product not found"
                ));
            }
            log.info("Product updated with id {}", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid product update request for id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "errorCode", 400,
                "errorMessage", e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/products/v1/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        log.debug("DELETE /api/products/v1/{} called", id);
        try {
            // Check if product exists before deletion
            if (productService.getProductById(id).isEmpty()) {
                log.warn("Product deletion failed: id {} not found", id);
                return ResponseEntity.status(404).body(Map.of(
                    "errorCode", 404,
                    "errorMessage", "Product not found"
                ));
            }
            productService.deleteProduct(id);
            log.info("Product deleted with id {}", id);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Product deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting product with id {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "errorCode", 500,
                "errorMessage", "Failed to delete product"
            ));
        }
    }
}
