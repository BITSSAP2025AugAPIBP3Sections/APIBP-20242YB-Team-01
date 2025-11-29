package com.core.auction_system.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidResponseDTO {
    private Integer id;
    private Integer amount;
    private LocalDateTime bidTime;
    private Integer productId;
    private Integer bidderId;
    private String email;
    private String reservationId;
    private String status;
}
