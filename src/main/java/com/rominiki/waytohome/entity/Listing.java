package com.rominiki.waytohome.entity;
import com.rominiki.waytohome.enums.ListingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
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
}