package com.core.auction_system.controller;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.service.BidService;
import com.core.auction_system.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/bids/v1")
public class BidController {

    private static final Logger log = LoggerFactory.getLogger(BidController.class);

    @Autowired
    private BidService bidService;

    @Autowired
    private ProductService productService;

    /**
     * GET /api/bids/v1
     */
    @GetMapping
    public List<Bid> getAllBids() {
        log.debug("GET /api/bids/v1 called");
        return bidService.getAllBids();
    }

    /**
     * GET /api/bids/v1/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Bid> getBidById(@PathVariable Integer id) {
        log.debug("GET /api/bids/v1/{} called", id);
        return bidService.getBidById(id)
                .map(b -> {
                    log.info("Bid found with id {}", id);
                    return ResponseEntity.ok(b);
                })
                .orElseGet(() -> {
                    log.warn("Bid not found for id {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * POST /api/bids/v1
     */
    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody Bid bid) {
        log.debug("POST /api/bids/v1 called with bid: {}", bid);

        Product product = bid.getProduct();
        if (product == null) {
            log.warn("Bid rejected: product required");
            return ResponseEntity.badRequest().body(Map.of("error", "Product required"));
        }
        if (bid.getAmount() < product.getMinBid()) {
            log.warn("Bid rejected: {} below minimum {}", bid.getAmount(), product.getMinBid());
            return ResponseEntity.badRequest().body(Map.of("error", "Bid below minimum"));
        }
        if (bid.getAmount() > product.getMaxBid()) {
            log.warn("Bid rejected: {} above maximum {}", bid.getAmount(), product.getMaxBid());
            return ResponseEntity.badRequest().body(Map.of("error", "Bid above maximum"));
        }

        Integer bidderId = bid.getBidderId();
        if (bidderId == null) {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Object details = auth.getDetails();
                log.debug("Extracting bidder id from SecurityContext details: {}", details);
                if (details instanceof Integer) {
                    bidderId = (Integer) details;
                } else if (details instanceof Number) {
                    bidderId = ((Number) details).intValue();
                } else if (details != null) {
                    try {
                        bidderId = Integer.parseInt(details.toString());
                    } catch (Exception ignored) {
                        log.error("Failed to parse bidder id from details: {}", details);
                    }
                }
            }
        }

        if (bidderId == null) {
            log.warn("Bid rejected: bidder id required");
            return ResponseEntity.badRequest().body(Map.of("error", "Bidder id required"));
        }

        bid.setBidderId(bidderId);

        if (bidService.hasUserBidOnProduct(bidderId, product)) {
            log.warn("Bid rejected: user {} already bid on product {}", bidderId, product.getId());
            return ResponseEntity.badRequest().body(Map.of("error", "User already bid"));
        }

        if (product.getFrozen() != null && product.getFrozen()) {
            log.warn("Bid rejected: product {} is frozen/auction closed", product.getId());
            return ResponseEntity.badRequest().body(Map.of("error", "Auction closed"));
        }

        if (bid.getAmount() <= product.getCurrentBid()) {
            log.warn("Bid rejected: {} not higher than current {}", bid.getAmount(), product.getCurrentBid());
            return ResponseEntity.badRequest().body(Map.of("error", "Bid not higher than current"));
        }

        Bid savedBid = bidService.placeBid(bid);
        product.setCurrentBid((double) bid.getAmount());
        productService.updateProduct(product.getId(), product);

        log.info("Bid placed successfully by user {} on product {} with amount {}", bidderId, product.getId(), bid.getAmount());
        return ResponseEntity.ok(savedBid);
    }
}
