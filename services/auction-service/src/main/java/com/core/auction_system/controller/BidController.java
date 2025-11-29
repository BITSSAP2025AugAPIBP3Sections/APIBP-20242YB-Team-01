package com.core.auction_system.controller;

import com.core.auction_system.dto.BidDTO;
import com.core.auction_system.dto.BidResponseDTO;
import com.core.auction_system.model.Bid;
import com.core.auction_system.service.BidService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bids/v1")
public class BidController {

    private static final Logger logger = LoggerFactory.getLogger(BidController.class);

    @Autowired
    private BidService bidService;

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
            return ResponseEntity.status(404).body(Map.of(
                "errorCode", 404,
                "errorMessage", "Bid not found"
            ));
        }
        logger.info("Bid found with id {}", id);
        return ResponseEntity.ok(opt.get());
    }

    /**
     * GET /api/bids/v1/user/{userId}
     * Get all bids placed by a specific user
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<BidResponseDTO>> getBidsByUser(@PathVariable Integer userId) {
        logger.debug("GET /api/bids/v1/users/{} called", userId);
        List<BidResponseDTO> bids = bidService.getBidsByBidderAsDTO(userId);
        logger.info("Found {} bids for user {}", bids.size(), userId);
        return ResponseEntity.ok(bids);
    }

    /**
     * GET /api/bids/v1/product/{productId}
     * Get all bids placed on a specific product
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<List<BidResponseDTO>> getBidsByProduct(@PathVariable Integer productId) {
        logger.debug("GET /api/bids/v1/products/{} called", productId);
        List<BidResponseDTO> bids = bidService.getBidsByProductIdAsDTO(productId);
        logger.info("Found {} bids for product {}", bids.size(), productId);
        return ResponseEntity.ok(bids);
    }

    /**
     * GET /api/bids/v1/users/{userId}/summary
     * Get bidding summary for a specific user with product details and status
     */
    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<List<com.core.auction_system.dto.BiddingSummaryDTO>> getBiddingSummary(@PathVariable Integer userId) {
        logger.debug("GET /api/bids/v1/users/{}/summary called", userId);
        List<com.core.auction_system.dto.BiddingSummaryDTO> summary = bidService.getBiddingSummaryByBidder(userId);
        logger.info("Found {} bids in summary for user {}", summary.size(), userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * POST /api/bids/v1
     * Accepts only amount and productId from user.
     * BuyerId and email are extracted from JWT token.
     * BidTime is set to current time.
     */
    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidDTO bidDto) {
        logger.debug("POST /api/bids/v1 called");

        // Extract authentication from JWT
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            logger.warn("Unauthenticated bid attempt");
            return ResponseEntity.status(401).body(Map.of(
                "errorCode", 401,
                "errorMessage", "Authentication required"
            ));
        }

        // Extract buyer ID from JWT token
        Integer bidderId = null;
        Object details = auth.getDetails();
        if (details instanceof Integer) {
            bidderId = (Integer) details;
        } else if (details instanceof Number) {
            bidderId = ((Number) details).intValue();
        } else if (details != null) {
            try {
                bidderId = Integer.parseInt(details.toString());
            } catch (NumberFormatException e) {
                logger.error("Invalid bidder ID format in JWT: {}", details);
                return ResponseEntity.status(401).body(Map.of(
                    "errorCode", 401,
                    "errorMessage", "Invalid authentication token"
                ));
            }
        }

        if (bidderId == null) {
            logger.error("Buyer ID not found in JWT token");
            return ResponseEntity.status(401).body(Map.of(
                "errorCode", 401,
                "errorMessage", "Authentication token missing user ID"
            ));
        }

        String email = auth.getName();
        logger.info("Processing bid from user: {} (ID: {})", email, bidderId);

        // Delegate to service layer
        BidService.BidPlacementResult result = bidService.placeBidWithValidation(bidDto, bidderId, email);

        if (result.isSuccess()) {
            Bid savedBid = result.getBid();
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Bid placed successfully");
            response.put("bidId", savedBid.getId());
            response.put("amount", savedBid.getAmount());
            response.put("productId", savedBid.getProduct().getId());
            return ResponseEntity.ok(response);
        } else {
            // Handle error responses from service
            java.util.Map<String, Object> errorResp = new java.util.LinkedHashMap<>();
            errorResp.put("errorCode", result.getStatusCode());
            errorResp.put("errorMessage", result.getErrorMessage());
            if (result.getErrorReason() != null) {
                errorResp.put("reason", result.getErrorReason());
            }
            return ResponseEntity.status(result.getStatusCode()).body(errorResp);
        }
    }
}
