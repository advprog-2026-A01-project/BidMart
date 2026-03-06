package id.ac.ui.cs.advprog.backend.bidding.model;

import java.time.LocalDateTime;

public class Bid {

    private Long auctionId;
    private Long bidderId;
    private Double amount;
    private LocalDateTime time;

    public Bid(Long auctionId, Long bidderId, Double amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.time = LocalDateTime.now();
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public Long getBidderId() {
        return bidderId;
    }

    public Double getAmount() {
        return amount;
    }

    public LocalDateTime getTime() {
        return time;
    }
}