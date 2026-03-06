package id.ac.ui.cs.advprog.backend.bidding.service;

import id.ac.ui.cs.advprog.backend.bidding.model.Bid;
import id.ac.ui.cs.advprog.backend.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.backend.bidding.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BidServiceTest {

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

        assertNotNull(bid, "Bid should not be null");
    }

    @Test
    void testBidLowerThanCurrentPriceMessage() {

        try {
            bidService.placeBid(1L, 10L, 9000.0);
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "Bid must be higher than current price",
                    e.getMessage(),
                    "Exception message mismatch"
            );
        }
    }

    @Test
    void testBidLowerThanCurrentPriceThrowsException() {

        assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(1L, 10L, 9000.0),
                "Should throw exception when bid lower than current price"
        );
    }

    @Test
    void testAuctionNotFoundThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(99L, 10L, 15000.0),
                "Should throw exception when auction not found"
        );
    }

    @Test
    void testAuctionNotFoundMessage() {

        try {
            bidService.placeBid(99L, 10L, 15000.0);
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "Auction not found",
                    e.getMessage(),
                    "Exception message mismatch"
            );
        }
    }
}