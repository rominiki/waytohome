package com.rominiki.waytohome.exception;

public class DuplicateConversationException extends RuntimeException {
    public DuplicateConversationException(Long listingId) {
        super("Conversation already exists for listing: " + listingId);
    }
}
