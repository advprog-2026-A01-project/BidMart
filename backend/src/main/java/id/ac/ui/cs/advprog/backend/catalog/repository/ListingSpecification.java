package id.ac.ui.cs.advprog.backend.catalog.repository;

import id.ac.ui.cs.advprog.backend.catalog.model.Listing;
import id.ac.ui.cs.advprog.backend.catalog.model.ListingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class ListingSpecification {
    // filter berdasarkan kata kunci di judul atau deskripsi
    public static Specification<Listing> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) return null;
            String lowerKeyword = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), lowerKeyword),
                    cb.like(cb.lower(root.get("description")), lowerKeyword)
            );
        };
    }

    // filter berdasarkan kategori
    public static Specification<Listing> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null :
                cb.equal(root.get("category").get("id"), categoryId);
    }

    // filter berdasarkan rentang harga
    public static Specification<Listing> priceBetween(Double min, Double max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("currentPrice"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("currentPrice"), min);
            return cb.lessThanOrEqualTo(root.get("currentPrice"), max);
        };
    }

    // filter berdasarkan daftar status yang diperbolehkan untuk muncul di publik
    public static Specification<Listing> hasStatusIn(List<ListingStatus> statuses) {
        return (root, query, cb) -> (statuses == null || statuses.isEmpty()) ? null :
                root.get("status").in(statuses);
    }

    // hanya tampilkan listing yang masih aktif/belum berakhir
    public static Specification<Listing> isNotExpired() {
        return (root, query, cb) -> cb.greaterThan(root.get("endTime"), LocalDateTime.now());
    }
}