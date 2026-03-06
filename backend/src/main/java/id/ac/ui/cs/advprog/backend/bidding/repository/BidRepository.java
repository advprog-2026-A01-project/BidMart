package id.ac.ui.cs.advprog.backend.bidding.repository;

import id.ac.ui.cs.advprog.backend.bidding.model.Bid;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class BidRepository {

    private List<Bid> bids = new ArrayList<>();

    public void save(Bid bid) {
        bids.add(bid);
    }

    public List<Bid> findByAuctionId(Long auctionId) {
        return bids.stream()
                .filter(b -> b.getAuctionId().equals(auctionId))
                .collect(Collectors.toList());
    }
}