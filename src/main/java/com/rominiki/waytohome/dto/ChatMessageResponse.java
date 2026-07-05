package com.rominiki.waytohome.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        Long recipientId,
        String recipientName,
        String content,
        LocalDateTime sentAt,
        boolean read
) {}