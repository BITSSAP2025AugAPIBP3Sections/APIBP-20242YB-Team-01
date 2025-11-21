package com.core.auction_system.service;

import com.core.auction_system.model.Product;
import com.core.auction_system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
// Duration no longer needed

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

    public Product createProduct(Product product) {
        if (product.getEndTime() != null && !isValidEndTime(product.getEndTime())) {
            throw new IllegalArgumentException("endTime must be on the hour and in the future");
        }
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
        if (endTime.getMinute() != 0 || endTime.getSecond() != 0 || endTime.getNano() != 0) return false;
        LocalDateTime now = LocalDateTime.now();
        if (!endTime.isAfter(now)) return false;
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
}
