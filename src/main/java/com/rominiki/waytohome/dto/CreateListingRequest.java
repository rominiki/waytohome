package com.rominiki.waytohome.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateListingRequest(
        @NotBlank String title,
        String description,
        @NotNull @Positive BigDecimal price,
        @NotBlank String location,
        @NotNull @Min(0) Integer bedrooms,
        Boolean petFriendly,
        Boolean accessible) {}