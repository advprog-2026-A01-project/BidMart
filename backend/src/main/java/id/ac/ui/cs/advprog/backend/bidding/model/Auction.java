package id.ac.ui.cs.advprog.backend.bidding.model;

public class Auction {

    private Long id;
    private Double currentPrice;
    private boolean active;

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

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public boolean isActive() {
        return active;
    }
}