package com.ebaazee.analytics_service.controller;

import com.ebaazee.analytics_service.dto.AuctionStatusEventDto;
import com.ebaazee.analytics_service.dto.NewBidEventDto;
import com.ebaazee.analytics_service.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/v1/events")
public class EventsController {

    private static final Logger log = LoggerFactory.getLogger(EventsController.class);

    private final AnalyticsService analyticsService;

    public EventsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/new-bid")
    public ResponseEntity<Void> newBid(@RequestBody NewBidEventDto event) {
        log.debug("POST /api/v1/events/new-bid with event: {}", event);
        analyticsService.processNewBid(event);
        log.info("Processed new bid event for auction {}", event.getAuctionId());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/auction-status")
    public ResponseEntity<Void> auctionStatus(@RequestBody AuctionStatusEventDto event) {
        log.debug("POST /api/v1/events/auction-status with event: {}", event);
        analyticsService.processAuctionStatus(event);
        log.info("Processed auction status event for auction {} with status {}", event.getAuctionId(),
                event.getStatus());
        return ResponseEntity.ok().build();
    }
}
