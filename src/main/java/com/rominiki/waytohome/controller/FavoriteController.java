package com.rominiki.waytohome.controller;
import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{listingId}")
    public ResponseEntity<Void> add(@PathVariable Long listingId, Authentication auth) {
        favoriteService.addFavorite(listingId, auth.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> remove(@PathVariable Long listingId, Authentication auth) {
        favoriteService.removeFavorite(listingId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ListingResponse> getAll(Authentication auth) {
        return favoriteService.getFavorites(auth.getName());
    }
}