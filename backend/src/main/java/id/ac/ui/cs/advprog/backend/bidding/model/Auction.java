package id.ac.ui.cs.advprog.backend.bidding.model;

public class Auction {

    private final Long id;
    private Double currentPrice;
    private final boolean active;

    public Auction(Long id, Double currentPrice, boolean active) {
        this.id = id;
        this.currentPrice = currentPrice;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void updatePrice(Double newPrice) {
        this.currentPrice = newPrice;
    }

    public boolean isActive() {
        return active;
    }

    public void placeBid(Double bidAmount) {
        if (!active) {
            throw new IllegalStateException("Auction is not active");
        }

        if (bidAmount <= currentPrice) {
            throw new IllegalArgumentException("Bid must be higher than current price");
        }

        currentPrice = bidAmount;
    }
}