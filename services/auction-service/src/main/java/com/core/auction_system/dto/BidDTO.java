package com.core.auction_system.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidDTO {
    private Integer amount;
    private Integer productId;
    private Integer id;
    private LocalDateTime bidTime;
    private String productName;
    private boolean isSold;
    private boolean isFrozen;
    private Integer buyerId;  
    private LocalDateTime endTime;
}
