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
    void testPlaceBidSuccess() {
        Bid bid = bidService.placeBid(1L, 10L, 12000.0);

        assertNotNull(bid, "Bid should not be null");
        assertEquals(12000.0, bid.getAmount());
        assertEquals(12000.0, auctionRepository.findById(1L).getCurrentPrice());
    }

    @Test
    void testBidLowerThanCurrentPriceThrowsExceptionAndMessage() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(1L, 10L, 9000.0),
                "Should throw exception when bid lower than current price"
        );
        assertEquals("Bid must be higher than current price", exception.getMessage());
    }

    @Test
    void testAuctionNotFoundThrowsExceptionAndMessage() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bidService.placeBid(99L, 10L, 15000.0),
                "Should throw exception when auction not found"
        );
        assertEquals("Auction not found", exception.getMessage());
    }

    @Test
    void testAuctionNotActiveThrowsException() {
        Auction auction = auctionRepository.findById(1L);
        auction.closeAuction(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bidService.placeBid(1L, 10L, 15000.0),
                "Should throw exception when auction is not active"
        );

        assertEquals("Auction is not active", exception.getMessage());
    }

    @Test
    void testGetBidHistory() {
        bidService.placeBid(1L, 10L, 12000.0);
        bidService.placeBid(1L, 11L, 15000.0);

        List<Bid> history = bidService.getBidHistory(1L);
        assertEquals(2, history.size());
    }

    @Test
    void testCloseAuctionSuccessWithWinner() {
        bidService.placeBid(1L, 10L, 15000.0);

        Auction closedAuction = bidService.closeAuction(1L);

        assertEquals(AuctionStatus.WON, closedAuction.getStatus());
        assertEquals(10L, closedAuction.getWinnerId());
    }

    @Test
    void testCloseAuctionUnsoldDueToReservePrice() {
        Auction customAuction = new Auction(2L, 1000.0, 50000.0, AuctionStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        auctionRepository.save(customAuction);

        bidService.placeBid(2L, 10L, 2000.0);

        Auction closedAuction = bidService.closeAuction(2L);

        assertEquals(AuctionStatus.UNSOLD, closedAuction.getStatus());
        assertNull(closedAuction.getWinnerId());
    }

    @Test
    void testGetWinningBid() {
        bidService.placeBid(1L, 10L, 12000.0);
        bidService.placeBid(1L, 11L, 15000.0);

        Bid winningBid = bidService.getWinningBid(1L);

        assertNotNull(winningBid);
        assertEquals(15000.0, winningBid.getAmount());
        assertEquals(11L, winningBid.getBidderId());
    }
}