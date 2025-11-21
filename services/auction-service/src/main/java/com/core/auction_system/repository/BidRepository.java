package com.core.auction_system.repository;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Integer> {
    boolean existsByBidderIdAndProduct(Integer bidderId, Product product);

    List<Bid> findByBidderId(Integer bidderId);

    List<Bid> findByProduct(Product product);

    Optional<Bid> findByReservationId(String reservationId);

    Optional<Bid> findTopByProductOrderByAmountDesc(Product product);

    long countByProduct(Product product);

    @Query("SELECT AVG(b.amount) FROM Bid b WHERE b.product = :product")
    Double findAverageBidAmountByProduct(Product product);
}
