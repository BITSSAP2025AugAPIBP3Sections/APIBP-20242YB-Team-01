package com.core.auction_system.service;

import com.core.auction_system.client.PaymentClient;
import com.core.auction_system.dto.BidDTO;
import com.core.auction_system.dto.BidResponseDTO;
import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.repository.BidRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BidService {
    private static final Logger logger = LoggerFactory.getLogger(BidService.class);

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentClient paymentClient;

    public List<Bid> getAllBids() {
        return bidRepository.findAll();
    }

    public Optional<Bid> getBidById(Integer id) {
        return bidRepository.findById(id);
    }

    public Bid placeBid(Bid bid) {
        return bidRepository.save(bid);
    }

    public List<Bid> getBidsByBidder(Integer bidderId) {
        return bidRepository.findByBidderId(bidderId);
    }

    public List<BidResponseDTO> getBidsByBidderAsDTO(Integer bidderId) {
        return mapToBidResponseDTOList(bidRepository.findByBidderId(bidderId));
    }

    public List<Bid> getBidsByProductId(Integer productId) {
        return bidRepository.findByProductId(productId);
    }

    public List<BidResponseDTO> getBidsByProductIdAsDTO(Integer productId) {
        return mapToBidResponseDTOList(bidRepository.findByProductId(productId));
    }

    public boolean hasUserBidOnProduct(Integer bidderId, Product product) {
        return bidRepository.existsByBidderIdAndProduct(bidderId, product);
    }

    /**
     * Convert Bid entity to BidResponseDTO with just product ID
     */
    private BidResponseDTO mapToBidResponseDTO(Bid bid) {
        BidResponseDTO dto = new BidResponseDTO();
        dto.setId(bid.getId());
        dto.setAmount(bid.getAmount());
        dto.setBidTime(bid.getBidTime());
        dto.setProductId(bid.getProduct() != null ? bid.getProduct().getId() : null);
        dto.setBidderId(bid.getBidderId());
        dto.setEmail(bid.getEmail());
        dto.setReservationId(bid.getReservationId());
        dto.setStatus(bid.getStatus());
        return dto;
    }

    /**
     * Convert list of Bid entities to list of BidResponseDTOs
     */
    private List<BidResponseDTO> mapToBidResponseDTOList(List<Bid> bids) {
        return bids.stream()
                .map(this::mapToBidResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get bidding summary for a specific bidder with product details and status
     */
    public List<com.core.auction_system.dto.BiddingSummaryDTO> getBiddingSummaryByBidder(Integer bidderId) {
        List<Bid> bids = bidRepository.findByBidderId(bidderId);
        
        return bids.stream()
                .map(bid -> {
                    com.core.auction_system.dto.BiddingSummaryDTO dto = new com.core.auction_system.dto.BiddingSummaryDTO();
                    dto.setId(bid.getId());
                    dto.setProductId(bid.getProduct() != null ? bid.getProduct().getId() : null);
                    dto.setProductName(bid.getProduct() != null ? bid.getProduct().getName() : "Unknown");
                    dto.setAmount(bid.getAmount());
                    dto.setBidTime(bid.getBidTime());
                    dto.setEndTime(bid.getProduct() != null ? bid.getProduct().getEndTime() : null);
                    
                    // Determine status
                    String status = "Active";
                    if (bid.getProduct() != null) {
                        Product product = bid.getProduct();
                        boolean isAuctionEnded = product.getFrozen() != null && product.getFrozen();
                        
                        if (isAuctionEnded) {
                            // Check if this bidder won
                            if (product.getBuyerId() != null && product.getBuyerId().equals(bidderId)) {
                                status = "Won";
                            } else {
                                status = "Lost";
                            }
                        } else {
                            // Auction still active - check if currently winning
                            if (product.getCurrentBid() != null && 
                                bid.getAmount().equals(product.getCurrentBid().intValue())) {
                                status = "Winning";
                            } else {
                                status = "Outbid";
                            }
                        }
                    }
                    
                    dto.setStatus(status);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Place a bid with full business logic including validation, payment reservation, and product update.
     *
     * @param bidDto   Contains amount and productId from user input
     * @param bidderId Buyer ID extracted from JWT token
     * @param email    Buyer email extracted from JWT token
     * @return BidPlacementResult containing the saved bid or error details
     * <p>
     * Note: bidTime is always set to current time, not from user input
     */
    public BidPlacementResult placeBidWithValidation(BidDTO bidDto, Integer bidderId, String email) {
        try {
            // Validate required fields in DTO
            if (bidDto.getProductId() == null) {
                return BidPlacementResult.error(400, "productId required");
            }
            if (bidDto.getAmount() == null) {
                return BidPlacementResult.error(400, "amount required");
            }
            if (bidderId == null) {
                return BidPlacementResult.error(400, "bidder id required");
            }

            // Load product by id
            Integer productId = bidDto.getProductId();
            Product product = productService.getProductById(productId).orElse(null);
            if (product == null) {
                return BidPlacementResult.error(400, "Product not found");
            }

            // Validate against product constraints
            if (product.getMinBid() == null || product.getMaxBid() == null) {
                return BidPlacementResult.error(400, "Product min/max required");
            }
            if (bidDto.getAmount() < product.getMinBid()) {
                return BidPlacementResult.error(400, "Bid below minimum");
            }
            if (bidDto.getAmount() > product.getMaxBid()) {
                return BidPlacementResult.error(400, "Bid above maximum");
            }

            // Check user hasn't already bid and auction status
            if (hasUserBidOnProduct(bidderId, product)) {
                return BidPlacementResult.error(400, "User already bid");
            }
            if (product.getFrozen() != null && product.getFrozen()) {
                return BidPlacementResult.error(400, "Auction closed");
            }
            // Check if auction has ended (current time is past end time)
            if (product.getEndTime() != null && LocalDateTime.now().isAfter(product.getEndTime())) {
                return BidPlacementResult.error(400, "Auction has ended");
            }
            if (product.getCurrentBid() != null && bidDto.getAmount() <= product.getCurrentBid()) {
                return BidPlacementResult.error(400, "Bid not higher than current");
            }

            // Reserve payment synchronously
            logger.info("Payment reserve: bidderId={} amount={} email={}", bidderId, bidDto.getAmount(), email);
            com.core.auction_system.client.PaymentClient.FreezeResponse fr =
                    paymentClient.freeze(bidderId, (double) bidDto.getAmount(), email);
            if (fr == null || !fr.ok) {
                String reason = fr == null ? "unknown" : (fr.reason == null ? "insufficient_funds" : fr.reason);
                return BidPlacementResult.error(402, "payment_reserve_failed", reason);
            }

            // Re-fetch the product (to reduce race window) and check currentBid again
            Product fresh = productService.getProductById(productId).orElse(null);
            if (fresh == null) {
                return BidPlacementResult.error(400, "Product not found");
            }
            if (fresh.getCurrentBid() != null && bidDto.getAmount() <= fresh.getCurrentBid()) {
                // Release reservation? For now, we return failure and let payment provider handle expiry.
                return BidPlacementResult.error(400, "Bid not higher than current (race detected)");
            }

            // Build Bid entity from DTO and save as PENDING
            // BidTime is always set to current time (not from user input)
            Bid bid = new Bid();
            bid.setAmount(bidDto.getAmount());
            bid.setProduct(fresh);
            bid.setBidderId(bidderId);  // From JWT token
            bid.setReservationId(fr.reservationId);
            bid.setStatus("PENDING");
            bid.setEmail(email);  // From JWT token
            bid.setBidTime(LocalDateTime.now());  // Always current time

            Bid savedBid = placeBid(bid);

            // update product currentBid to the new amount
            fresh.setCurrentBid((double) bidDto.getAmount());
            productService.saveProduct(fresh);

            // finalization happens asynchronously on payment.success
            logger.info("Bid placed successfully: bidId={} productId={} amount={}", savedBid.getId(), productId,
                    bidDto.getAmount());
            return BidPlacementResult.success(savedBid);
        } catch (Exception ex) {
            logger.error("Error in placeBidWithValidation", ex);
            return BidPlacementResult.error(500, "internal_server_error", ex.getMessage());
        }
    }

    /**
     * Result object for bid placement operations.
     */
    public static class BidPlacementResult {
        private final boolean success;
        private final Bid bid;
        private final int statusCode;
        private final String errorMessage;
        private final String errorReason;

        private BidPlacementResult(boolean success, Bid bid, int statusCode, String errorMessage, String errorReason) {
            this.success = success;
            this.bid = bid;
            this.statusCode = statusCode;
            this.errorMessage = errorMessage;
            this.errorReason = errorReason;
        }

        public static BidPlacementResult success(Bid bid) {
            return new BidPlacementResult(true, bid, 200, null, null);
        }

        public static BidPlacementResult error(int statusCode, String errorMessage) {
            return new BidPlacementResult(false, null, statusCode, errorMessage, null);
        }

        public static BidPlacementResult error(int statusCode, String errorMessage, String errorReason) {
            return new BidPlacementResult(false, null, statusCode, errorMessage, errorReason);
        }

        public boolean isSuccess() {
            return success;
        }

        public Bid getBid() {
            return bid;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getErrorReason() {
            return errorReason;
        }
    }
}
