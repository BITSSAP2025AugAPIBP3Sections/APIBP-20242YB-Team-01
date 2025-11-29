package com.core.auction_system.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidDTO {
    // Required fields from user
    private Integer amount;
    private Integer productId;

    // Optional fields for response/internal use
    private Integer id;
    private LocalDateTime bidTime;
    private String productName;
    private boolean isSold;
    private boolean isFrozen;
    private Integer buyerId;  // Will be extracted from JWT, not from user input
    private LocalDateTime endTime;
    // Lombok will generate constructors, getters, setters
}
