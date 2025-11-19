package com.core.auction_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Integer id;
    private String name;
    private String description;
    private String category;
    private Double minBid;
    private Double maxBid;
    private Double currentBid;
    // Lombok will generate constructors, getters, setters
}
