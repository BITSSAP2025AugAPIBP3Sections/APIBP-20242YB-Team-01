package com.core.auction_system.controller;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.service.BidService;
import com.core.auction_system.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/bids/v1")
public class BidController {

    private static final Logger logger = LoggerFactory.getLogger(BidController.class);

    @Autowired
    private BidService bidService;

    @Autowired
    private ProductService productService;
    @Autowired
    private com.core.auction_system.client.PaymentClient paymentClient;

    /**
     * GET /api/bids/v1
     */
    @GetMapping
    public List<Bid> getAllBids() {
        logger.debug("GET /api/bids/v1 called");
        return bidService.getAllBids();
    }

    /**
     * GET /api/bids/v1/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Bid> getBidById(@PathVariable Integer id) {
        logger.debug("GET /api/bids/v1/{} called", id);
        return bidService.getBidById(id)
                .map(b -> {
                    logger.info("Bid found with id {}", id);
                    return ResponseEntity.ok(b);
                })
                .orElseGet(() -> {
                    logger.warn("Bid not found for id {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * POST /api/bids/v1
     */
    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody Bid bid) {
        try {
            // Business rule: validate bid
            Product product = bid.getProduct();
            if (product == null) return ResponseEntity.badRequest().body("Product required");
            if (bid.getAmount() == null) return ResponseEntity.badRequest().body("Amount required");
            if (product.getMinBid() == null || product.getMaxBid() == null) return ResponseEntity.badRequest().body("Product min/max required");
            if (bid.getAmount() < product.getMinBid()) return ResponseEntity.badRequest().body("Bid below minimum");
            if (bid.getAmount() > product.getMaxBid()) return ResponseEntity.badRequest().body("Bid above maximum");
            Integer bidderId = bid.getBidderId();
        if (bidderId == null) {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Object details = auth.getDetails();
                logger.debug("Extracting bidder id from SecurityContext details: {}", details);
                if (details instanceof Integer) {
                    bidderId = (Integer) details;
                } else if (details instanceof Number) {
                    bidderId = ((Number) details).intValue();
                } else if (details != null) {
                    try {
                        bidderId = Integer.parseInt(details.toString());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (bidderId == null) return ResponseEntity.badRequest().body("Bidder id required");
        // Set bidder id back into bid
        bid.setBidderId(bidderId);
        if (bidService.hasUserBidOnProduct(bidderId, product))
            return ResponseEntity.badRequest().body("User already bid");
            if (product.getFrozen() != null && product.getFrozen())
                return ResponseEntity.badRequest().body("Auction closed");
            if (product.getCurrentBid() != null && bid.getAmount() <= product.getCurrentBid())
                return ResponseEntity.badRequest().body("Bid not higher than current");
            // Reserve payment synchronously (include user email from token if available)
            String email = null;
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) email = auth.getName();
            // Log resolved email and bid details for debugging
            logger.info("Payment reserve: bidderId={} amount={} email={}", bidderId, bid.getAmount(), email);
            com.core.auction_system.client.PaymentClient.FreezeResponse fr = paymentClient.freeze(bidderId, (double) bid.getAmount(), email);
        if (fr == null || !fr.ok) {
            String reason = fr == null ? "unknown" : (fr.reason == null ? "insufficient_funds" : fr.reason);
            return ResponseEntity.status(402).body(Map.of("error", "payment_reserve_failed", "reason", reason));
        }
    // Save bid as PENDING with reservationId
    bid.setReservationId(fr.reservationId);
    bid.setStatus("PENDING");
    if (bid.getBidTime() == null) bid.setBidTime(LocalDateTime.now());
    Bid savedBid = bidService.placeBid(bid);
        // Do not finalize product state here; finalization will happen on payment.success event
        return ResponseEntity.ok(savedBid);
        } catch (Exception ex) {
            logger.error("Error in placeBid", ex);
            return ResponseEntity.status(500).body(Map.of("error", "internal_server_error", "message", ex.getMessage()));
        }
    }
}
