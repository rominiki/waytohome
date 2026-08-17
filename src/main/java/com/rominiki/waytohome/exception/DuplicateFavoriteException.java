package com.rominiki.waytohome.exception;

public class DuplicateFavoriteException extends RuntimeException {
    public DuplicateFavoriteException(Long listingId) {
        super("Listing already favorited: " + listingId);
    }
}
