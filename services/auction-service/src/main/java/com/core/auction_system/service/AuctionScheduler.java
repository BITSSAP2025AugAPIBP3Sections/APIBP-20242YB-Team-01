package com.core.auction_system.service;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.repository.BidRepository;
import com.core.auction_system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class AuctionScheduler {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BidRepository bidRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> productsToCheck = productRepository.findByEndTimeBeforeAndFrozenFalse(now);
        for (Product product : productsToCheck) {
            List<Bid> bids = bidRepository.findByProduct(product);
            if (bids.isEmpty()) {
                product.setFrozen(true);
                productRepository.save(product);
            } else {
                Bid highestBid = bids.stream()
                        .max(Comparator.comparing(Bid::getAmount))
                        .orElse(null);
                if (highestBid != null) {
                    product.setSold(true);
                    product.setBuyerId(highestBid.getBidderId());
                    product.setFrozen(true);
                    productRepository.save(product);
                }
            }
        }
    }
}
