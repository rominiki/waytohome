package com.rominiki.waytohome.dto;

import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.enums.ListingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListingResponse(
        Long id, String title, String description,
        BigDecimal price, String location,
        Integer bedrooms, Boolean petFriendly,
        ListingStatus status,
        String ownerName, // flattened — not the full User object
        LocalDateTime createdAt) {
    public static ListingResponse from(Listing l) {
        return new ListingResponse(
                l.getId(), l.getTitle(), l.getDescription(),
                l.getPrice(), l.getLocation(), l.getBedrooms(),
                l.getPetFriendly(), l.getStatus(),
                l.getOwner().getFullName(),
                l.getCreatedAt());
    }
}
