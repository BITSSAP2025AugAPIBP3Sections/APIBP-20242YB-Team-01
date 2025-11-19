package com.core.auction_system;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Category;
import com.core.auction_system.model.Product;
import com.core.auction_system.repository.BidRepository;
import com.core.auction_system.repository.CategoryRepository;
import com.core.auction_system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BidRepository bidRepository;


    @Override
    public void run(String... args) throws Exception {
        // seed categories
        if (categoryRepository.count() == 0) {
            Category c1 = new Category(null, "Electronics");
            Category c2 = new Category(null, "Books");
            Category c3 = new Category(null, "Home");
            categoryRepository.saveAll(Arrays.asList(c1, c2, c3));
        }

        // seed products/bids with user id references (auth is external service)
        if (productRepository.count() == 0) {
            // We'll use fake integer ids for sellers/buyers in dev. In production these should map to auth-svc user ids.
            Integer seller1Id = 101;
            Integer seller2Id = 102;
            Integer buyer1Id = 201;

            // seed products
            Product p1 = new Product();
            p1.setName("iPhone 14");
            p1.setDescription("Used iPhone 14 in good condition");
            p1.setCategory("Electronics");
            p1.setMinBid(200.0);
            p1.setMaxBid(2000.0);
            p1.setCurrentBid(0.0);
            p1.setEndTime(LocalDateTime.now().plusDays(2));
            p1.setSellerId(seller1Id);

            Product p2 = new Product();
            p2.setName("Java Programming Book");
            p2.setDescription("Comprehensive guide to Java 21");
            p2.setCategory("Books");
            p2.setMinBid(10.0);
            p2.setMaxBid(200.0);
            p2.setCurrentBid(0.0);
            p2.setEndTime(LocalDateTime.now().plusHours(12));
            p2.setSellerId(seller2Id);

            productRepository.saveAll(Arrays.asList(p1, p2));

            // seed bids
            Bid b1 = new Bid();
            b1.setAmount(250);
            b1.setBidTime(LocalDateTime.now());
            b1.setProduct(p1);
            b1.setBidderId(buyer1Id);

            bidRepository.save(b1);

            // update product current bid and persist
            p1.setCurrentBid((double) b1.getAmount());
            productRepository.save(p1);
        }
    }
}
