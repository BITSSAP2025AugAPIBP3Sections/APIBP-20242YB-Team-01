package com.core.auction_system.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiddingSummaryDTO {
    private Integer id; // Bid ID
    private Integer productId;
    private String productName;
    private Integer amount;
    private String status; // "Winning", "Outbid", "Lost", "Won"
    private LocalDateTime bidTime;
    private LocalDateTime endTime; // Product end time
}
