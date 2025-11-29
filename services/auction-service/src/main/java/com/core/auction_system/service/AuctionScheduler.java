package com.core.auction_system.service;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.repository.BidRepository;
import com.core.auction_system.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private com.core.auction_system.client.PaymentClient paymentClient;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("AuctionScheduler triggered at {}", now);

        List<Product> productsToCheck = productRepository.findByEndTimeBeforeAndFrozenFalse(now);
        log.info("Found {} products to evaluate for closing", productsToCheck.size());

        for (Product product : productsToCheck) {
            log.debug("Checking product id {} - '{}'", product.getId(), product.getName());
            List<Bid> bids = bidRepository.findByProduct(product);

            if (bids.isEmpty()) {
                log.info("No bids found for product id {}. Marking as closed (unsold).", product.getId());
                product.setFrozen(true);
                try {
                    productRepository.save(product);
                    log.info("DB update SUCCESS: Product id {} marked as frozen (unsold) and saved.", product.getId());
                } catch (Exception e) {
                    log.error("DB update FAILED: Could not save product id {} as frozen (unsold): {}", product.getId(),
                            e.getMessage(), e);
                }
            } else {
                Bid highestBid = bids.stream()
                        .max(Comparator.comparing(Bid::getAmount))
                        .orElse(null);

                if (highestBid != null) {
                    // Try to finalize payment by attempting to deduct from bidders in descending order
                    List<Bid> sorted = bids.stream().sorted(Comparator.comparing(Bid::getAmount).reversed()).toList();
                    boolean finalized = false;
                    for (Bid candidate : sorted) {
                        Integer buyerId = candidate.getBidderId();
                        Double amount = candidate.getAmount().doubleValue();
                        String reservationId = candidate.getReservationId();
                        log.info("Trying to deduct {} from user {} (reservation={}) for product {}", amount, buyerId,
                                reservationId, product.getId());
                        com.core.auction_system.client.PaymentClient.GenericResponse dr =
                                paymentClient.deduct(buyerId, amount, product.getId(), reservationId,
                                        candidate.getEmail());
                        if (dr != null && dr.ok) {
                            // success: mark product sold to this bidder
                            log.info("Deduct succeeded for user {} amount {} reservation {}", buyerId, amount,
                                    reservationId);
                            product.setSold(true);
                            product.setBuyerId(buyerId);
                            product.setFrozen(true);
                            try {
                                productRepository.save(product);
                                log.info(
                                        "DB update SUCCESS: Product id {} marked as sold, buyer set to {}, and frozen.",
                                        product.getId(), buyerId);
                            } catch (Exception e) {
                                log.error("DB update FAILED: Could not save product id {} as sold: {}", product.getId(),
                                        e.getMessage(), e);
                            }

                            // deposit to seller
                            try {
                                Integer sellerId = product.getSellerId();
                                if (sellerId != null) {
                                    com.core.auction_system.client.PaymentClient.GenericResponse dep =
                                            paymentClient.deposit(sellerId, amount, "auction_sale");
                                    if (dep != null && dep.ok) {
                                        log.info("Deposited {} to seller {} for product {}", amount, sellerId,
                                                product.getId());
                                    } else {
                                        log.warn("Failed to deposit to seller {} for product {}: {}", sellerId,
                                                product.getId(), dep == null ? "null" : dep.reason);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Exception depositing to seller for product {}: {}", product.getId(),
                                        e.getMessage());
                            }

                            // publish auction.winner and auction.seller events with emails when possible
                            try (Connection conn = new ConnectionFactory() {{
                                setUri(System.getenv()
                                        .getOrDefault("RABBITMQ_URL", "amqp://guest:guest@rabbitmq:5672"));
                            }}.newConnection();
                                 Channel ch = conn.createChannel()) {
                                ch.exchangeDeclare("events", "topic", true);
                                ObjectMapper mapper = new ObjectMapper();

                                // try to resolve buyer email
                                String buyerEmail = candidate.getEmail();
                                ObjectNode winner = mapper.createObjectNode();
                                winner.put("userId", String.valueOf(candidate.getBidderId()));
                                winner.put("email", buyerEmail == null ? "" : buyerEmail);
                                winner.put("amount", candidate.getAmount());
                                winner.put("auctionId", product.getId());
                                winner.put("reservationId",
                                        candidate.getReservationId() == null ? "" : candidate.getReservationId());
                                winner.put("message", "Congratulations! You won the auction for '" + product.getName() +
                                        "' with bid " + candidate.getAmount());
                                // winner.put("ts", System.currentTimeMillis());
                                ch.basicPublish("events", "auction.winner", MessageProperties.PERSISTENT_TEXT_PLAIN,
                                        mapper.writeValueAsBytes(winner));

                                // try to resolve seller email
                                // String sellerEmail = lookupEmail(product.getSellerId());
                                // ObjectNode sellerMsg = mapper.createObjectNode();
                                // sellerMsg.put("userId", product.getSellerId() == null ? "" : String.valueOf(product.getSellerId()));
                                // sellerMsg.put("email", sellerEmail == null ? "" : sellerEmail);
                                // sellerMsg.put("amount", candidate.getAmount());
                                // sellerMsg.put("auctionId", product.getId());
                                // // seller doesn't have a reservationId for this event; include empty string
                                // sellerMsg.put("reservationId", "");
                                // sellerMsg.put("message", "Your product '" + product.getName() + "' was sold for " + candidate.getAmount());
                                // // sellerMsg.put("ts", System.currentTimeMillis());
                                // ch.basicPublish("events", "auction.seller", MessageProperties.PERSISTENT_TEXT_PLAIN, mapper.writeValueAsBytes(sellerMsg));
                            } catch (Exception e) {
                                log.warn("Failed to publish auction notifications for product {}: {}", product.getId(),
                                        e.getMessage());
                            }

                            // unfreeze other bidders
                            for (Bid other : sorted) {
                                if (other.getId().equals(candidate.getId())) {
                                    continue;
                                }
                                try {
                                    if (other.getReservationId() != null) {
                                        com.core.auction_system.client.PaymentClient.GenericResponse ur =
                                                paymentClient.unfreeze(other.getBidderId(),
                                                        other.getAmount().doubleValue());
                                        if (ur != null && ur.ok) {
                                            log.info("Unfroze {} for user {} (reservation={})", other.getAmount(),
                                                    other.getBidderId(), other.getReservationId());
                                        } else {
                                            log.warn("Failed to unfreeze for user {} reservation {}: {}",
                                                    other.getBidderId(), other.getReservationId(),
                                                    ur == null ? "null" : ur.reason);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("Exception unfreezing reservation for user {}: {}", other.getBidderId(),
                                            e.getMessage());
                                }
                            }

                            finalized = true;
                            break;
                        } else {
                            log.warn("Deduct failed for user {} amount {} reservation {}: {}", buyerId, amount,
                                    reservationId, dr == null ? "null" : dr.reason);
                        }
                    }

                    if (!finalized) {
                        // none of the deducts succeeded; mark closed (frozen) and attempt to unfreeze all
                        log.warn("No deduct succeeded for product {}. Marking closed and unfreezing reservations.",
                                product.getId());
                        product.setFrozen(true);
                        try {
                            productRepository.save(product);
                            log.info(
                                    "DB update SUCCESS: Product id {} marked as frozen (no successful deduct) and saved.",
                                    product.getId());
                        } catch (Exception e) {
                            log.error(
                                    "DB update FAILED: Could not save product id {} as frozen (no successful deduct): {}",
                                    product.getId(), e.getMessage(), e);
                        }
                        for (Bid other : bids) {
                            try {
                                if (other.getReservationId() != null) {
                                    paymentClient.unfreeze(other.getBidderId(), other.getAmount().doubleValue());
                                }
                            } catch (Exception e) {
                                log.warn("Failed to unfreeze reservation for user {}: {}", other.getBidderId(),
                                        e.getMessage());
                            }
                        }
                    }
                } else {
                    log.warn("Unexpected state: bids present but no highest bid found for product id {}",
                            product.getId());
                }
            }
        }
    }
}
