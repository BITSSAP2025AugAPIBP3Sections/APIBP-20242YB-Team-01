package com.core.auction_system.service;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BidService {
    @Autowired
    private BidRepository bidRepository;

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

    public List<Bid> getBidsByProduct(Product product) {
        return bidRepository.findByProduct(product);
    }

    public boolean hasUserBidOnProduct(Integer bidderId, Product product) {
        return bidRepository.existsByBidderIdAndProduct(bidderId, product);
    }

    public Optional<Bid> getHighestBid(Product product) {
        return bidRepository.findTopByProductOrderByAmountDesc(product);
    }

    public long countBidsForProduct(Product product) {
        return bidRepository.countByProduct(product);
    }

    public Double getAverageBidAmount(Product product) {
        return bidRepository.findAverageBidAmountByProduct(product);
    }
}
