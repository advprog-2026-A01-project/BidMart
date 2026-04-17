package id.ac.ui.cs.advprog.backend.catalog.service;

import id.ac.ui.cs.advprog.backend.catalog.model.Listing;
import id.ac.ui.cs.advprog.backend.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.backend.catalog.repository.ListingRepository;
import id.ac.ui.cs.advprog.backend.catalog.repository.ListingSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listingRepository;

    public List<Listing> searchListings(String keyword, Long categoryId, Double minPrice, Double maxPrice) {
        // status yang boleh publik: ACTIVE & EXTENDED
        List<ListingStatus> activeStatuses = List.of(ListingStatus.ACTIVE, ListingStatus.EXTENDED);

        Specification<Listing> spec = Specification
                .where(ListingSpecification.hasStatusIn(activeStatuses))
                .and(ListingSpecification.isNotExpired())
                .and(ListingSpecification.hasKeyword(keyword))
                .and(ListingSpecification.hasCategory(categoryId))
                .and(ListingSpecification.priceBetween(minPrice, maxPrice));

        return listingRepository.findAll(spec);
    }
}