package id.ac.ui.cs.advprog.backend.bidding.service;

import id.ac.ui.cs.advprog.backend.bidding.model.Auction;
import id.ac.ui.cs.advprog.backend.bidding.model.Bid;
import id.ac.ui.cs.advprog.backend.bidding.enums.AuctionStatus;
import id.ac.ui.cs.advprog.backend.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.backend.bidding.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidServiceTest {

    private BidService bidService;
    private AuctionRepository auctionRepository;
    private BidRepository bidRepository;

    @BeforeEach
    void setUp() {
        auctionRepository = new AuctionRepository();
        bidRepository = new BidRepository();
        bidService = new BidService(auctionRepository, bidRepository);
    }

    @Test
    void testPlaceBidSuccessReturnsNotNull() {
        Bid bid = bidService.placeBid(1L, 10L, 12000.0);
        assertNotNull(bid, "Bid should not be null after successfully placing a bid");
    }

    @Test
    void testPlaceBidSuccessHasCorrectAmount() {
        Bid bid = bidService.placeBid(1L, 10L, 12000.0);
        assertEquals(12000.0, bid.getAmount(), "Bid amount should match the placed amount");
    }

    @Test
    void testPlaceBidSuccessUpdatesAuctionPrice() {
        bidService.placeBid(1L, 10L, 12000.0);
        assertEquals(12000.0, auctionRepository.findById(1L).getCurrentPrice(), "Auction current price should be updated");
    }

    @Test
    void testBidLowerThanCurrentPriceThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(1L, 10L, 9000.0),
                "Should throw exception when bid is lower than current price"
        );
    }

    @Test
    void testBidLowerThanCurrentPriceExceptionMessage() {
        try {
            bidService.placeBid(1L, 10L, 9000.0);
        } catch (IllegalArgumentException e) {
            assertEquals("Bid must be higher than current price", e.getMessage(), "Exception message mismatch for low bid");
        }
    }

    @Test
    void testAuctionNotFoundThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(99L, 10L, 15000.0),
                "Should throw exception when auction is not found"
        );
    }

    @Test
    void testAuctionNotFoundExceptionMessage() {
        try {
            bidService.placeBid(99L, 10L, 15000.0);
        } catch (IllegalArgumentException e) {
            assertEquals("Auction not found", e.getMessage(), "Exception message mismatch for not found auction");
        }
    }

    @Test
    void testAuctionNotActiveThrowsException() {
        Auction auction = auctionRepository.findById(1L);
        auction.closeAuction(null);

        assertThrows(
                IllegalStateException.class,
                () -> bidService.placeBid(1L, 10L, 15000.0),
                "Should throw exception when placing bid on inactive auction"
        );
    }

    @Test
    void testAuctionNotActiveExceptionMessage() {
        Auction auction = auctionRepository.findById(1L);
        auction.closeAuction(null);

        try {
            bidService.placeBid(1L, 10L, 15000.0);
        } catch (IllegalStateException e) {
            assertEquals("Auction is not active", e.getMessage(), "Exception message mismatch for inactive auction");
        }
    }

    @Test
    void testGetBidHistorySize() {
        bidService.placeBid(1L, 10L, 12000.0);
        bidService.placeBid(1L, 11L, 15000.0);

        List<Bid> history = bidService.getBidHistory(1L);
        assertEquals(2, history.size(), "Bid history size should be 2");
    }

    @Test
    void testCloseAuctionSuccessWithWinnerStatus() {
        bidService.placeBid(1L, 10L, 15000.0);
        Auction closedAuction = bidService.closeAuction(1L);

        assertEquals(AuctionStatus.WON, closedAuction.getStatus(), "Auction status should be WON");
    }

    @Test
    void testCloseAuctionSuccessWithWinnerId() {
        bidService.placeBid(1L, 10L, 15000.0);
        Auction closedAuction = bidService.closeAuction(1L);

        assertEquals(10L, closedAuction.getWinnerId(), "Winning bidder ID should be 10L");
    }

    @Test
    void testCloseAuctionUnsoldDueToReservePriceStatus() {
        Auction customAuction = new Auction(2L, 1000.0, 50000.0, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        auctionRepository.save(customAuction);
        bidService.placeBid(2L, 10L, 2000.0);
        Auction closedAuction = bidService.closeAuction(2L);

        assertEquals(AuctionStatus.UNSOLD, closedAuction.getStatus(), "Auction status should be UNSOLD if reserve price is not met");
    }

    @Test
    void testCloseAuctionUnsoldDueToReservePriceWinnerNull() {
        Auction customAuction = new Auction(2L, 1000.0, 50000.0, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        auctionRepository.save(customAuction);
        bidService.placeBid(2L, 10L, 2000.0);
        Auction closedAuction = bidService.closeAuction(2L);

        assertNull(closedAuction.getWinnerId(), "Winner ID should be null if auction is unsold");
    }

    @Test
    void testGetWinningBidNotNull() {
        bidService.placeBid(1L, 10L, 12000.0);
        bidService.placeBid(1L, 11L, 15000.0);
        Bid winningBid = bidService.getWinningBid(1L);

        assertNotNull(winningBid, "Winning bid should not be null");
    }

    @Test
    void testGetWinningBidAmount() {
        bidService.placeBid(1L, 10L, 12000.0);
        bidService.placeBid(1L, 11L, 15000.0);
        Bid winningBid = bidService.getWinningBid(1L);

        assertEquals(15000.0, winningBid.getAmount(), "Winning bid amount should be the highest placed bid");
    }

    @Test
    void testGetWinningBidderId() {
        bidService.placeBid(1L, 10L, 12000.0);
        bidService.placeBid(1L, 11L, 15000.0);
        Bid winningBid = bidService.getWinningBid(1L);

        assertEquals(11L, winningBid.getBidderId(), "Winning bidder ID should be 11L");
    }
}