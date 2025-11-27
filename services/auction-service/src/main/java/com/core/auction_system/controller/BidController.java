package com.core.auction_system.controller;

import com.core.auction_system.dto.BidDTO;
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
    public ResponseEntity<?> getBidById(@PathVariable Integer id) {
        logger.debug("GET /api/bids/v1/{} called", id);
        var opt = bidService.getBidById(id);
        if (opt.isEmpty()) {
            logger.warn("Bid not found for id {}", id);
            return ResponseEntity.status(404).body(Map.of("error", "Bid not found"));
        }
        logger.info("Bid found with id {}", id);
        return ResponseEntity.ok(opt.get());
    }

    /**
     * POST /api/bids/v1
     */
    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidDTO bidDto) {
        try {
            // Validate required fields in DTO
            if (bidDto.getProductId() == null) return ResponseEntity.badRequest().body(Map.of("error", "productId required"));
            if (bidDto.getAmount() == null) return ResponseEntity.badRequest().body(Map.of("error", "amount required"));

            // Load product by id
            Integer productId = bidDto.getProductId();
            Product product = productService.getProductById(productId).orElse(null);
            if (product == null) return ResponseEntity.badRequest().body(Map.of("error", "Product not found"));

            // Validate against product constraints
            if (product.getMinBid() == null || product.getMaxBid() == null) return ResponseEntity.badRequest().body(Map.of("error", "Product min/max required"));
            if (bidDto.getAmount() < product.getMinBid()) return ResponseEntity.badRequest().body(Map.of("error", "Bid below minimum"));
            if (bidDto.getAmount() > product.getMaxBid()) return ResponseEntity.badRequest().body(Map.of("error", "Bid above maximum"));

            // Resolve bidder id from DTO or security context
            Integer bidderId = bidDto.getBuyerId();
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
                        try { bidderId = Integer.parseInt(details.toString()); } catch (Exception ignored) {}
                    }
                }
            }
            if (bidderId == null) return ResponseEntity.badRequest().body(Map.of("error", "bidder id required"));

            // Check user hasn't already bid and auction status
            if (bidService.hasUserBidOnProduct(bidderId, product)) return ResponseEntity.badRequest().body(Map.of("error", "User already bid"));
            if (product.getFrozen() != null && product.getFrozen()) return ResponseEntity.badRequest().body(Map.of("error", "Auction closed"));
            if (product.getCurrentBid() != null && bidDto.getAmount() <= product.getCurrentBid()) return ResponseEntity.badRequest().body(Map.of("error", "Bid not higher than current"));

            // Determine email from auth if available (for notifications/payment metadata)
            String email = null;
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) email = auth.getName();

            // Reserve payment synchronously
            logger.info("Payment reserve: bidderId={} amount={} email={}", bidderId, bidDto.getAmount(), email);
            com.core.auction_system.client.PaymentClient.FreezeResponse fr = paymentClient.freeze(bidderId, (double) bidDto.getAmount(), email);
            if (fr == null || !fr.ok) {
                String reason = fr == null ? "unknown" : (fr.reason == null ? "insufficient_funds" : fr.reason);
                return ResponseEntity.status(402).body(Map.of("error", "payment_reserve_failed", "reason", reason));
            }
            // Re-fetch the product (to reduce race window) and check currentBid again
            Product fresh = productService.getProductById(productId).orElse(null);
            if (fresh == null) return ResponseEntity.badRequest().body(Map.of("error", "Product not found"));
            if (fresh.getCurrentBid() != null && bidDto.getAmount() <= fresh.getCurrentBid()) {
                // Release reservation? For now, we return failure and let payment provider handle expiry.
                return ResponseEntity.badRequest().body(Map.of("error", "Bid not higher than current (race detected)"));
            }

            // Build Bid entity from DTO and save as PENDING
            Bid bid = new Bid();
            bid.setAmount(bidDto.getAmount());
            bid.setProduct(fresh);
            bid.setBidderId(bidderId);
            bid.setReservationId(fr.reservationId);
            bid.setStatus("PENDING");
            bid.setEmail(email);
            bid.setBidTime(bidDto.getBidTime() == null ? LocalDateTime.now() : bidDto.getBidTime());

            Bid savedBid = bidService.placeBid(bid);

            // update product currentBid to the new amount
            fresh.setCurrentBid((double) bidDto.getAmount());
            productService.saveProduct(fresh);

            // finalization happens asynchronously on payment.success
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("status", "success");
            resp.put("message", "Bid placed");
            resp.put("bidId", savedBid.getId());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            logger.error("Error in placeBid", ex);
            return ResponseEntity.status(500).body(Map.of("error", "internal_server_error", "message", ex.getMessage()));
        }
    }
}
