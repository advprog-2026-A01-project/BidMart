package id.ac.ui.cs.advprog.backend.bidding.model;

import id.ac.ui.cs.advprog.backend.bidding.enums.AuctionStatus;

import java.time.LocalDateTime;

public class Auction {

    private final Long id;
    private Double currentPrice;
    private final Double reservePrice;
    private AuctionStatus status;
    private LocalDateTime endTime;
    private Long winnerId;

    public Auction(Long id, Double currentPrice, Double reservePrice,
                   AuctionStatus status, LocalDateTime endTime) {
        this.id = id;
        this.currentPrice = currentPrice;
        this.reservePrice = reservePrice;
        this.status = status;
        this.endTime = endTime;
        this.winnerId = null;
    }

    public Auction(Long id, Double currentPrice, boolean active) {
        this.id = id;
        this.currentPrice = currentPrice;
        this.reservePrice = 0.0;
        this.status = active ? AuctionStatus.ACTIVE : AuctionStatus.CLOSED;
        this.endTime = LocalDateTime.now().plusDays(1);
        this.winnerId = null;
    }

    public Long getId() {
        return id;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public Double getReservePrice() {
        return reservePrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void updatePrice(Double newPrice) {
        this.currentPrice = newPrice;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isActive() {
        return status == AuctionStatus.ACTIVE || status == AuctionStatus.EXTENDED;
    }

    public void placeBid(Double bidAmount) {
        if (!isActive()) {
            throw new IllegalStateException("Auction is not active");
        }

        if (bidAmount <= currentPrice) {
            throw new IllegalArgumentException("Bid must be higher than current price");
        }

        currentPrice = bidAmount;
    }

    public void closeAuction(Long highestBidderId) {
        if (!isActive()) {
            throw new IllegalStateException("Auction is not active");
        }

        this.status = AuctionStatus.CLOSED;

        if (highestBidderId != null && currentPrice >= reservePrice) {
            this.winnerId = highestBidderId;
            this.status = AuctionStatus.WON;
        } else {
            this.status = AuctionStatus.UNSOLD;
        }
    }
}