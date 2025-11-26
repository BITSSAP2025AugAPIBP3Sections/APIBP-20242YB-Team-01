package com.ebaazee.analytics_service.controller;

import com.ebaazee.analytics_service.service.AnalyticsService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * /api/analytics/v1
 * <p>
 * Endpoints:
 * GET /api/analytics/v1/bidders/top?limit=2
 * GET /api/analytics/v1/auctions/popular?limit=2
 * GET /api/analytics/v1/auctions/{auctionId}/stats
 */
@RestController
@RequestMapping(path = "/api/analytics/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class AnalyticsController {

    // added
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/top-bidders")
    public ResponseEntity<List<Map<String, Object>>> topBidders(@RequestParam(defaultValue = "2") int limit) {
        log.debug("GET /api/v1/analytics/top-bidders called with limit {}", limit);
        List<Map<String, Object>> result = analyticsService.getTopBidders(limit);
        log.info("Returning {} top bidders", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/auctions/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularAuctions(
            @RequestParam(name = "limit", defaultValue = "2") int limit) {
        log.debug("GET /api/analytics/v1/auctions/popular called with limit {}", limit);
        List<Map<String, Object>> result = analyticsService.getPopularAuctions(limit);
        log.info("Returning {} popular auctions", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/auctions/{auctionId}/stats")
    public ResponseEntity<Map<String, Object>> getAuctionStats(@PathVariable String auctionId) {
        log.debug("GET /api/analytics/v1/auctions/{}/stats called", auctionId);
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
