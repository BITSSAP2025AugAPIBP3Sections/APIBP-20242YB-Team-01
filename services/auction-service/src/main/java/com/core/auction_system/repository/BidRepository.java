package com.core.auction_system.repository;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Integer> {
    boolean existsByBidderIdAndProduct(Integer bidderId, Product product);

    List<Bid> findByBidderId(Integer bidderId);

    List<Bid> findByProduct(Product product);

    List<Bid> findByProductId(Integer productId);

    Optional<Bid> findByReservationId(String reservationId);

    Optional<Bid> findTopByProductOrderByAmountDesc(Product product);
}
