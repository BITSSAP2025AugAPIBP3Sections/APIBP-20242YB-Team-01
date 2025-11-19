package com.core.auction_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidDTO {
    private Integer id;
    private Integer amount;
    private LocalDateTime bidTime;
    private Integer productId;
    private String productName;
    private boolean isSold;
    private boolean isFrozen;
    private Integer buyerId;
    private LocalDateTime endTime;
    // Lombok will generate constructors, getters, setters
}
