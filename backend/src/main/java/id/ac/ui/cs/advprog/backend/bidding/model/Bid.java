package id.ac.ui.cs.advprog.backend.bidding.model;

import java.time.LocalDateTime;

public class Bid {

    private final Long auctionId;
    private final Long bidderId;
    private final Double amount;
    private final LocalDateTime time;

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

    public boolean isFromBidder(Long bidderId) {
        return this.bidderId.equals(bidderId);
    }

    public boolean isForAuction(Long auctionId) {
        return this.auctionId.equals(auctionId);
    }

    public boolean isHigherThanAmount(Double amount) {
        return this.amount > amount;
    }
}