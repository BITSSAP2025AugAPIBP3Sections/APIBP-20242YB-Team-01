package com.core.auction_system.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new product.
 * Only accepts user input fields. Seller information is extracted from JWT token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateDTO {
    private String name;
    private String description;
    private String category;
    private Double minBid;
    private Double maxBid;
    private LocalDateTime endTime;  // Must be on the hour (e.g., 12:00, 1:00, 2:00)
}
