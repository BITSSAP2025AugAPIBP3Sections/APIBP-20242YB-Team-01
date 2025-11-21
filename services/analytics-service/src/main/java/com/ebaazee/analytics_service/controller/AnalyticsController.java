package com.ebaazee.analytics_service.controller;

import com.ebaazee.analytics_service.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {

    // added
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/top-bidders")
    public ResponseEntity<List<Map<String,Object>>> topBidders(@RequestParam(defaultValue = "2") int limit) {
        log.debug("GET /api/v1/analytics/top-bidders called with limit {}", limit);
        List<Map<String,Object>> result = analyticsService.getTopBidders(limit);
        log.info("Returning {} top bidders", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analytics/popular-auctions")
    public ResponseEntity<List<Map<String,Object>>> popularAuctions(@RequestParam(defaultValue = "2") int limit) {
        log.debug("GET /api/v1/analytics/popular-auctions called with limit {}", limit);
        List<Map<String,Object>> result = analyticsService.getPopularAuctions(limit);
        log.info("Returning {} popular auctions", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/auctions/{auctionId}/stats")
    public ResponseEntity<Map<String,Object>> auctionStats(@PathVariable String auctionId) {
        log.debug("GET /api/v1/auctions/{}/stats called", auctionId);
        return analyticsService.getAuctionStats(auctionId)
                .map(stats -> {
                    log.info("Stats found for auction {}", auctionId);
                    return ResponseEntity.ok(stats);
                })
                .orElseGet(() -> {
                    log.warn("No stats found for auction {}", auctionId);
                    return ResponseEntity.notFound().build();
                });
    }
}
