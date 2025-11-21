package com.core.auction_system.service;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.repository.BidRepository;
import com.core.auction_system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AuctionScheduler {

    // added
    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BidRepository bidRepository;

    @Scheduled(cron = "0 0 * * * *")
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
                productRepository.save(product);
            } else {
                Bid highestBid = bids.stream()
                        .max(Comparator.comparing(Bid::getAmount))
                        .orElse(null);

                if (highestBid != null) {
                    log.info("Product id {} sold to user {} with bid {}",
                            product.getId(), highestBid.getBidderId(), highestBid.getAmount());
                    product.setSold(true);
                    product.setBuyerId(highestBid.getBidderId());
                    product.setFrozen(true);
                    productRepository.save(product);
                } else {
                    log.warn("Unexpected state: bids present but no highest bid found for product id {}", product.getId());
                }
            }
        }
    }
}
