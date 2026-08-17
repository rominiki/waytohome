package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.entity.*;
import com.rominiki.waytohome.exception.DuplicateFavoriteException;
import com.rominiki.waytohome.exception.ResourceNotFoundException;
import com.rominiki.waytohome.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    @Transactional
    public void addFavorite(Long listingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        if (favoriteRepository.existsByUserAndListing(user, listing)) {
            throw new DuplicateFavoriteException(listingId);
        }
        favoriteRepository.save(Favorite.builder()
                .user(user)
                .listing(listing)
                .build());
    }

    @Transactional
    public void removeFavorite(Long listingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        
        // Check if favorite exists before attempting to delete
        if (!favoriteRepository.existsByUserAndListing(user, listing)) {
            throw new ResourceNotFoundException("Favorite not found");
        }
        
        favoriteRepository.deleteByUserAndListing(user, listing);
    }

    public List<ListingResponse> getFavorites(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return favoriteRepository.findByUser(user).stream()
                .map(f -> ListingResponse.from(f.getListing()))
                .toList();
    }
}
