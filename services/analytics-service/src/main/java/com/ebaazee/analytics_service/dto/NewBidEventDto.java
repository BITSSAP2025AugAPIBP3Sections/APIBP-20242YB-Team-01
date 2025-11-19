package com.ebaazee.analytics_service.dto;

import lombok.Data;

@Data
public class NewBidEventDto {
    private String auctionId;
    private String bidId;
    private String userId;
    private double amount;
    private String timestamp;
}
