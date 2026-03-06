package id.ac.ui.cs.advprog.backend.bidding.service;

import id.ac.ui.cs.advprog.backend.bidding.model.Auction;
import id.ac.ui.cs.advprog.backend.bidding.model.Bid;
import id.ac.ui.cs.advprog.backend.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.backend.bidding.repository.BidRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
            throw new RuntimeException("Auction not found");
        }

        if (!auction.isActive()) {
            throw new RuntimeException("Auction not active");
        }

        if (amount <= auction.getCurrentPrice()) {
            throw new RuntimeException("Bid must be higher than current price");
        }

        Bid bid = new Bid(auctionId, bidderId, amount);

        bidRepository.save(bid);

        auction.setCurrentPrice(amount);
        auctionRepository.save(auction);

        return bid;
    }

    public List<Bid> getBidHistory(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId);
    }
}