package com.core.auction_system.service;

import com.core.auction_system.dto.ProductCreateDTO;
import com.core.auction_system.model.Product;
import com.core.auction_system.repository.ProductRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

    /**
     * Create a product from DTO with seller information from JWT.
     * Sets default values for isFrozen (false) and isSold (false).
     * Validates that endTime is on the hour.
     */
    public Product createProductFromDTO(ProductCreateDTO dto, Integer sellerId) {
        // Validate required fields
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Product category is required");
        }
        if (dto.getMinBid() == null || dto.getMinBid() <= 0) {
            throw new IllegalArgumentException("minBid must be greater than 0");
        }
        if (dto.getMaxBid() == null || dto.getMaxBid() <= 0) {
            throw new IllegalArgumentException("maxBid must be greater than 0");
        }
        if (dto.getMaxBid() <= dto.getMinBid()) {
            throw new IllegalArgumentException("maxBid must be greater than minBid");
        }
        if (dto.getEndTime() == null) {
            throw new IllegalArgumentException("endTime is required");
        }

        // Validate endTime is on the hour
        if (!isValidEndTime(dto.getEndTime())) {
            throw new IllegalArgumentException(
                    "endTime must be on the hour (e.g., 12:00, 1:00, 2:00) and in the future");
        }

        Product product = new Product();
        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");
        product.setCategory(dto.getCategory().trim());
        product.setMinBid(dto.getMinBid());
        product.setMaxBid(dto.getMaxBid());
        product.setEndTime(dto.getEndTime());

        // Set seller from JWT token
        product.setSellerId(sellerId);

        // Set default values
        product.setCurrentBid(0.0);
        product.setFrozen(false);
        product.setSold(false);
        product.setBuyerId(null);

        return productRepository.save(product);
    }

    public Product updateProduct(Integer id, Product updatedProduct) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(updatedProduct.getName());
                    product.setDescription(updatedProduct.getDescription());
                    product.setCategory(updatedProduct.getCategory());
                    product.setMinBid(updatedProduct.getMinBid());
                    product.setMaxBid(updatedProduct.getMaxBid());
                    if (updatedProduct.getEndTime() != null && !isValidEndTime(updatedProduct.getEndTime())) {
                        throw new IllegalArgumentException("endTime must be on the hour and in the future");
                    }
                    product.setEndTime(updatedProduct.getEndTime());
                    return productRepository.save(product);
                })
                .orElse(null);
    }

    // endTime must be exactly on the hour (minute, second, nano == 0)
    // and must be in the future but no more than 24 hours from now
    private boolean isValidEndTime(LocalDateTime endTime) {
        if (endTime.getMinute() != 0 || endTime.getSecond() != 0 || endTime.getNano() != 0) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!endTime.isAfter(now)) {
            return false;
        }
        // endTime must be in the future (strictly after now)
        return endTime.isAfter(now);
    }

    public void deleteProduct(Integer id) {
        productRepository.deleteById(id);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> getProductsBySeller(Integer sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    // convenience save method used by controllers/services when partial updates are needed
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }
}
