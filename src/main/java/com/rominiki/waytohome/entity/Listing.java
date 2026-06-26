package com.rominiki.waytohome.entity;
import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.enums.ListingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Integer bedrooms;

    private Boolean petFriendly;

    private Boolean accessible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void approve() {
        if (this.status == ListingStatus.REJECTED) {
            throw new IllegalStateException("Cannot approve an already-rejected listing");
        }
        this.status = ListingStatus.APPROVED;
    }

    public void updateFrom(CreateListingRequest req) {
        this.title = req.title();
        this.description = req.description();
        this.price = req.price();
        this.location = req.location();
        this.bedrooms = req.bedrooms();
        this.petFriendly = req.petFriendly();
        this.accessible = req.accessible();
    }
}