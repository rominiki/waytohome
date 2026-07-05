package com.rominiki.waytohome.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long listingId,
        String listingTitle,
        Long studentId,
        String studentName,
        Long landlordId,
        String landlordName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
