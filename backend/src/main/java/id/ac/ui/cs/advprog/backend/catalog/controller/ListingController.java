package id.ac.ui.cs.advprog.backend.catalog.controller;

import id.ac.ui.cs.advprog.backend.catalog.model.Listing;
import id.ac.ui.cs.advprog.backend.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {
    private final ListingService listingService;

    @GetMapping("/search")
    public List<Listing> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        return listingService.searchListings(keyword, categoryId, minPrice, maxPrice);
    }
}