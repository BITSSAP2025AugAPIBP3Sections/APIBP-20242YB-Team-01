package com.core.auction_system.controller;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.service.BidService;
import com.core.auction_system.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
public class BidController {
    @Autowired
    private BidService bidService;
    @Autowired
    private ProductService productService;

    @GetMapping
    public List<Bid> getAllBids() {
        return bidService.getAllBids();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bid> getBidById(@PathVariable Integer id) {
        return bidService.getBidById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody Bid bid) {
        // Business rule: validate bid
        Product product = bid.getProduct();
        if (product == null) return ResponseEntity.badRequest().body("Product required");
        if (bid.getAmount() < product.getMinBid()) return ResponseEntity.badRequest().body("Bid below minimum");
        if (bid.getAmount() > product.getMaxBid()) return ResponseEntity.badRequest().body("Bid above maximum");
        Integer bidderId = bid.getBidderId();
        if (bidderId == null) {
            // Try to extract from SecurityContext (set by JwtAuthorizationFilter)
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Object details = auth.getDetails();
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
        if (bid.getAmount() <= product.getCurrentBid())
            return ResponseEntity.badRequest().body("Bid not higher than current");
        // Place bid
        Bid savedBid = bidService.placeBid(bid);
        // Update product current bid
        product.setCurrentBid((double) bid.getAmount());
        productService.updateProduct(product.getId(), product);
        return ResponseEntity.ok(savedBid);
    }
}
