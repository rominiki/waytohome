package com.rominiki.waytohome.dto;

import jakarta.validation.constraints.NotNull;

public record StartConversationRequest(
        @NotNull Long listingId
) {}
