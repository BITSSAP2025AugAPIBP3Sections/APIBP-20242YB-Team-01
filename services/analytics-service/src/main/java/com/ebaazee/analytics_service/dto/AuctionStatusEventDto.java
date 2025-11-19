package com.ebaazee.analytics_service.dto;

import lombok.Data;

@Data
public class AuctionStatusEventDto {
    private String auctionId;
    private String status;
    private String startTime;
    private String endTime;
}
