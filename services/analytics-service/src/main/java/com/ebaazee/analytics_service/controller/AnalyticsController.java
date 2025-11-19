package com.ebaazee.analytics_service.controller;

import com.ebaazee.analytics_service.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/top-bidders")
    public ResponseEntity<List<Map<String,Object>>> topBidders(@RequestParam(defaultValue = "2") int limit) {
        return ResponseEntity.ok(analyticsService.getTopBidders(limit));
    }

    @GetMapping("/analytics/popular-auctions")
    public ResponseEntity<List<Map<String,Object>>> popularAuctions(@RequestParam(defaultValue = "2") int limit) {
        return ResponseEntity.ok(analyticsService.getPopularAuctions(limit));
    }

    @GetMapping("/auctions/{auctionId}/stats")
    public ResponseEntity<Map<String,Object>> auctionStats(@PathVariable String auctionId) {
        return analyticsService.getAuctionStats(auctionId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
