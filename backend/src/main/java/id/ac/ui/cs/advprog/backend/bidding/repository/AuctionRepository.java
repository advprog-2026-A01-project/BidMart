package id.ac.ui.cs.advprog.backend.bidding.repository;

import id.ac.ui.cs.advprog.backend.bidding.model.Auction;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class AuctionRepository {

    private final Map<Long, Auction> auctions = new HashMap<>();

    public AuctionRepository() {
        auctions.put(1L, new Auction(1L, 10000.0, true));
    }

    public Auction findById(Long id) {
        return auctions.get(id);
    }

    public void save(Auction auction) {
        auctions.put(auction.getId(), auction);
    }
}