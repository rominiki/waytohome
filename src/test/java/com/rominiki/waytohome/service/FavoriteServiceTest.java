package com.rominiki.waytohome.service;

import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.entity.Favorite;
import com.rominiki.waytohome.repository.FavoriteRepository;
import com.rominiki.waytohome.repository.ListingRepository;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock FavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;
    @Mock ListingRepository listingRepository;
    @InjectMocks FavoriteService favoriteService;

    @Test
    void addFavorite_whenNotExists_savesIt() {
        var user = User.builder().email("s@test.com").build();
        var listing = Listing.builder().id(1L).build();
        when(userRepository.findByEmail("s@test.com"))
                .thenReturn(Optional.of(user));
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserAndListing(user, listing))
                .thenReturn(false);
        favoriteService.addFavorite(1L, "s@test.com");
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void addFavorite_whenAlreadyExists_doesNotSaveAgain() {
        var user = User.builder().email("s@test.com").build();
        var listing = Listing.builder().id(1L).build();
        when(userRepository.findByEmail("s@test.com"))
                .thenReturn(Optional.of(user));
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserAndListing(user, listing))
                .thenReturn(true); // already favorited
        favoriteService.addFavorite(1L, "s@test.com");
        verify(favoriteRepository, never()).save(any());
    }
}
