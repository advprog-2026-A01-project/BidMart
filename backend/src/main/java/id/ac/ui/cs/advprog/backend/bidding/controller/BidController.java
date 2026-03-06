package id.ac.ui.cs.advprog.backend.bidding.controller;

import id.ac.ui.cs.advprog.backend.bidding.model.Bid;
import id.ac.ui.cs.advprog.backend.bidding.service.BidService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    public Bid placeBid(
            @RequestParam Long auctionId,
            @RequestParam Long bidderId,
            @RequestParam Double amount) {

        return bidService.placeBid(auctionId, bidderId, amount);
    }

    @GetMapping("/{auctionId}")
    public List<Bid> getBidHistory(@PathVariable Long auctionId) {
        return bidService.getBidHistory(auctionId);
    }
}