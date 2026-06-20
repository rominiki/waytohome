package com.rominiki.waytohome.dto;
import java.math.BigDecimal;
public record ListingSearchCriteria(
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String location,
        Boolean petFriendly,
        Integer bedrooms) {}