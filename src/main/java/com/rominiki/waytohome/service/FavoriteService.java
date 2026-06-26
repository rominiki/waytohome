package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.entity.*;
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
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Listing listing = listingRepository.findById(listingId).orElseThrow();
        if (favoriteRepository.existsByUserAndListing(user, listing)) {
            return; // no error, no duplicate — just do nothing
        }
        favoriteRepository.save(Favorite.builder()
                .user(user).listing(listing).build());
    }

    @Transactional
    public void removeFavorite(Long listingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        Listing listing = listingRepository.findById(listingId).orElseThrow();
        favoriteRepository.deleteByUserAndListing(user, listing);
    }

    public List<ListingResponse> getFavorites(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow();
        return favoriteRepository.findByUser(user).stream()
                .map(f -> ListingResponse.from(f.getListing()))
                .toList();
    }
}
