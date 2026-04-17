package id.ac.ui.cs.advprog.backend.bidding.service;

import id.ac.ui.cs.advprog.backend.bidding.model.Auction;
import id.ac.ui.cs.advprog.backend.bidding.model.Bid;
import id.ac.ui.cs.advprog.backend.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.backend.bidding.repository.BidRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public BidService(AuctionRepository auctionRepository, BidRepository bidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    public Bid placeBid(Long auctionId, Long bidderId, Double amount) {
        Auction auction = auctionRepository.findById(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        auction.placeBid(amount);

        Bid bid = new Bid(auctionId, bidderId, amount);
        bidRepository.save(bid);
        auctionRepository.save(auction);

        return bid;
    }

    public List<Bid> getBidHistory(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId);
    }

    public Auction closeAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        if (!auction.isActive()) {
            throw new IllegalArgumentException("Auction is not active");
        }

        Optional<Bid> highestBid = bidRepository.findByAuctionId(auctionId)
                .stream()
                .max(Comparator.comparingDouble(Bid::getAmount));

        Long highestBidderId = highestBid.map(Bid::getBidderId).orElse(null);

        auction.closeAuction(highestBidderId);
        auctionRepository.save(auction);

        return auction;
    }

    public Bid getWinningBid(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId)
                .stream()
                .max(Comparator.comparingDouble(Bid::getAmount))
                .orElse(null);
    }
}