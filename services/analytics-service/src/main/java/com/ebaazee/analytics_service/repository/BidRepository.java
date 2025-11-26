package com.ebaazee.analytics_service.repository;

import com.ebaazee.analytics_service.model.Bid;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Integer> {

    /**
     * Top bidders: bidder_id, total_bids, total_amount
     * Returns List<Object[]> where each row is [bidder_id, total_bids, total_amount]
     */
    @Query(value = "SELECT bidder_id AS bidderId, COUNT(*) AS total_bids, SUM(amount) AS total_amount " +
            "FROM bids " +
            "GROUP BY bidder_id " +
            "ORDER BY SUM(amount) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopBiddersNative(@Param("limit") int limit);

    /**
     * Popular auctions: product_id, product_name, total_bids, highest_bid
     * Returns List<Object[]> where each row is [product_id, product_name, total_bids, highest_bid]
     */
    @Query(value =
            "SELECT b.product_id AS productId, p.name AS title, COUNT(*) AS total_bids, MAX(b.amount) AS highest_bid " +
                    "FROM bids b " +
                    "JOIN products p ON b.product_id = p.id " +
                    "GROUP BY b.product_id, p.name " +
                    "ORDER BY COUNT(*) DESC " +
                    "LIMIT :limit", nativeQuery = true)
    List<Object[]> findPopularAuctionsNative(@Param("limit") int limit);

    /**
     * Auction stats stats: total_bids, highest_bid, lowest_bid, average_bid
     * Returns List<Object[]> - first row contains the aggregates.
     * Using List<Object[]> is safer across JPA providers.
     */
    @Query(value =
            "SELECT COUNT(*) AS total_bids, MAX(amount) AS highest_bid, MIN(amount) AS lowest_bid, AVG(amount) AS average_bid " +
                    "FROM bids WHERE product_id = :productId", nativeQuery = true)
    List<Object[]> findAuctionStatsNativeList(@Param("productId") int productId);

    // convenience method to fetch bids for a product
    List<Bid> findByProductId(Integer productId);
}
