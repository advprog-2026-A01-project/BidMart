package id.ac.ui.cs.advprog.backend.bidding.service;

import id.ac.ui.cs.advprog.backend.bidding.model.Bid;
import id.ac.ui.cs.advprog.backend.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.backend.bidding.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BidServiceTest {

    private BidService bidService;

    @BeforeEach
    void setUp() {
        AuctionRepository auctionRepository = new AuctionRepository();
        BidRepository bidRepository = new BidRepository();

        bidService = new BidService(auctionRepository, bidRepository);
    }

    @Test
    void testPlaceBidSuccess() {

        Bid bid = bidService.placeBid(1L, 10L, 12000.0);

        assertNotNull(bid);
        assertEquals(12000.0, bid.getAmount());
        assertEquals(1L, bid.getAuctionId());
    }

    @Test
    void testBidLowerThanCurrentPrice() {

        Exception exception = assertThrows(RuntimeException.class, () -> {
            bidService.placeBid(1L, 10L, 9000.0);
        });

        assertEquals("Bid must be higher than current price", exception.getMessage());
    }

    @Test
    void testAuctionNotFound() {

        Exception exception = assertThrows(RuntimeException.class, () -> {
            bidService.placeBid(99L, 10L, 15000.0);
        });

        assertEquals("Auction not found", exception.getMessage());
    }

}