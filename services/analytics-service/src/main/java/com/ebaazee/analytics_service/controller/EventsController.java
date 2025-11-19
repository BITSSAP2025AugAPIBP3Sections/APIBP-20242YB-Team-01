package com.ebaazee.analytics_service.controller;

import com.ebaazee.analytics_service.dto.NewBidEventDto;
import com.ebaazee.analytics_service.dto.AuctionStatusEventDto;
import com.ebaazee.analytics_service.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventsController {

    private final AnalyticsService analyticsService;

    public EventsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/new-bid")
    public ResponseEntity<Void> newBid(@RequestBody NewBidEventDto event) {
        analyticsService.processNewBid(event);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/auction-status")
    public ResponseEntity<Void> auctionStatus(@RequestBody AuctionStatusEventDto event) {
        analyticsService.processAuctionStatus(event);
        return ResponseEntity.ok().build();
    }
}
