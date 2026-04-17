package id.ac.ui.cs.advprog.backend.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_listings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @Column(nullable = false)
    private Double startingPrice;

    private Double reservePrice;

    @Column(nullable = false)
    private Integer durationMinutes;

    private LocalDateTime endTime;

    @Column(nullable = false)
    private String sellerId;

    private Double currentPrice;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ListingStatus status = ListingStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // helper method untuk mengecek apakah listing bisa diubah atau dibatalkan
    public boolean canBeModified() {
        // penjual hanya dapat memperbarui atau membatalkan selama belum ada penawaran
        // logic: jika harga current = harga awal, diasumsikan belum ada bid
        return currentPrice == null || currentPrice.equals(startingPrice);
    }
}