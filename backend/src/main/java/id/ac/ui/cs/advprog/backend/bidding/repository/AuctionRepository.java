package id.ac.ui.cs.advprog.backend.bidding.repository;

import id.ac.ui.cs.advprog.backend.bidding.model.Auction;
import id.ac.ui.cs.advprog.backend.bidding.enums.AuctionStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class AuctionRepository {

    private final Map<Long, Auction> auctions = new HashMap<>();

    public AuctionRepository() {
        auctions.put(1L, new Auction(1L, 10000.0, 5000.0,
                AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1)));
    }

    public Auction findById(Long id) {
        return auctions.get(id);
    }

    public void save(Auction auction) {
        auctions.put(auction.getId(), auction);
    }
}