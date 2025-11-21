package com.ebaazee.analytics_service.service;

import com.ebaazee.analytics_service.dto.AuctionStatusEventDto;
import com.ebaazee.analytics_service.dto.NewBidEventDto;
import com.ebaazee.analytics_service.model.Product;
import com.ebaazee.analytics_service.repository.BidRepository;
import com.ebaazee.analytics_service.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.*;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AnalyticsService - DB-backed analytics logic.
 *
 * Notes:
 * - This service assumes BidRepository provides native aggregation queries:
 *   - findTopBiddersNative(int limit) -> List<Object[]>
 *   - findPopularAuctionsNative(int limit) -> List<Object[]>
 *   - findAuctionStatsNativeList(int productId) -> List<Object[]>
 *
 * - The service is read-only for existing auction tables. processNewBid/processAuctionStatus
 *   only log/validate events (they do not write to the auction DB).
 */
@Service
public class AnalyticsService {

    // added
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final UserClientService userClientService;

    public AnalyticsService(BidRepository bidRepository,
                            ProductRepository productRepository,
                            UserClientService userClientService) {
        this.bidRepository = bidRepository;
        this.productRepository = productRepository;
        this.userClientService = userClientService;
    }

    /**
     * Called by EventsController when a new-bid event is posted.
     * NOTE: Analytics is read-only on the auction DB, so this method currently logs/validates only.
     */
    public void processNewBid(NewBidEventDto event) {
        log.debug("Received new-bid event: {}", event);
        if (event == null) {
            log.warn("Received null event in processNewBid()");
            return;
        }
        try {
            if (event.getTimestamp() != null) {
                Instant.parse(event.getTimestamp());
            }
        } catch (DateTimeException ex) {
            log.warn("Invalid timestamp in new-bid event: {}", event.getTimestamp());
        }
    }

    /**
     * Called by EventsController when an auction-status event is posted.
     * Currently logs/validates only (read-only).
     */
    public void processAuctionStatus(AuctionStatusEventDto event) {
        log.debug("Received auction-status event: {}", event);
        if (event == null) {
            log.warn("Received null event in processAuctionStatus()");
        }
    }

    /**
     * Returns top bidders as a list of maps:
     *  - userId, name, totalBids, totalAmount
     */
    public List<Map<String, Object>> getTopBidders(int limit) {
        log.debug("Fetching top {} bidders", limit);
        List<Object[]> rows = bidRepository.findTopBiddersNative(limit);
        List<Map<String, Object>> result = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            log.info("No bidders found for analytics");
            return result;
        }

        for (Object[] r : rows) {
            Integer bidderId = r[0] == null ? null : toInteger(r[0]);
            long totalBids = r[1] == null ? 0L : toLong(r[1]);
            double totalAmount = r[2] == null ? 0.0 : toDouble(r[2]);

            String name = bidderId != null ? userClientService.getUserName(String.valueOf(bidderId)) : "Unknown";

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", bidderId);
            m.put("name", name);
            m.put("totalBids", totalBids);
            m.put("totalAmount", totalAmount);
            result.add(m);
        }
        log.info("Top bidders result size: {}", result.size());
        return result;
    }

    /**
     * Returns popular auctions (products):
     *  - auctionId, title, totalBids, highestBid
     */
    public List<Map<String, Object>> getPopularAuctions(int limit) {
        log.debug("Fetching top {} popular auctions", limit);
        List<Object[]> rows = bidRepository.findPopularAuctionsNative(limit);
        List<Map<String, Object>> result = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            log.info("No popular auctions found for analytics");
            return result;
        }

        for (Object[] r : rows) {
            Integer productId = r[0] == null ? null : toInteger(r[0]);
            String title = r[1] == null ? null : r[1].toString();
            long totalBids = r[2] == null ? 0L : toLong(r[2]);
            double highestBid = r[3] == null ? 0.0 : toDouble(r[3]);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("auctionId", productId);
            m.put("title", title);
            m.put("totalBids", totalBids);
            m.put("highestBid", highestBid);
            result.add(m);
        }
        log.info("Popular auctions result size: {}", result.size());
        return result;
    }

    /**
     * Returns auction statistics (Optional).
     */
    public Optional<Map<String, Object>> getAuctionStats(String auctionIdStr) {
        log.debug("Fetching auction stats for id '{}'", auctionIdStr);
        try {
            int auctionId = Integer.parseInt(auctionIdStr);

            List<Object[]> rows = bidRepository.findAuctionStatsNativeList(auctionId);
            Object[] statsRow = (rows == null || rows.isEmpty()) ? null : rows.get(0);

            long totalBids = 0L;
            double highest = 0.0;
            double lowest = 0.0;
            double avg = 0.0;

            if (statsRow != null) {
                totalBids = statsRow.length > 0 ? toLong(statsRow[0]) : 0L;
                highest = statsRow.length > 1 ? toDouble(statsRow[1]) : 0.0;
                lowest = statsRow.length > 2 ? toDouble(statsRow[2]) : 0.0;
                avg = statsRow.length > 3 ? toDouble(statsRow[3]) : 0.0;
            }

            Map<String, Object> m = new LinkedHashMap<>();
            Optional<Product> pOpt = productRepository.findById(auctionId);
            if (pOpt.isPresent()) {
                Product p = pOpt.get();
                m.put("auctionId", p.getId());
                m.put("title", p.getName());
                m.put("startTime", p.getEndTime()); // adjust if model changes
                m.put("endTime", p.getEndTime());
                log.info("Stats found for auction {}", auctionId);
            } else {
                log.warn("No product found for auction id {}", auctionId);
                m.put("auctionId", auctionId);
                m.put("title", null);
                m.put("startTime", null);
                m.put("endTime", null);
            }

            m.put("totalBids", totalBids);
            m.put("highestBid", highest);
            m.put("lowestBid", lowest);
            m.put("averageBid", avg);

            return Optional.of(m);
        } catch (NumberFormatException ex) {
            log.warn("Invalid auction id '{}'", auctionIdStr);
            return Optional.empty();
        }
    }

    /* ------------------ Helper converters ------------------ */

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); }
        catch (NumberFormatException e) {
            try { return (long) Double.parseDouble(o.toString()); }
            catch (NumberFormatException ex) { return 0L; }
        }
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private Integer toInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); }
        catch (NumberFormatException ex) { return null; }
    }
}
