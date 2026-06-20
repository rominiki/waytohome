package com.rominiki.waytohome.controller;
import com.rominiki.waytohome.dto.*;
import com.rominiki.waytohome.service.ListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PostMapping
    @PreAuthorize("hasRole('LANDLORD')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ListingResponse> create(
            @Valid @RequestBody CreateListingRequest req,
            Authentication auth) {
        var result = listingService.create(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public Page<ListingResponse> getAll(@PageableDefault(size = 10) Pageable pageable) {
        return listingService.getApproved(pageable);
    }

    @GetMapping("/{id}")
    public ListingResponse getOne(@PathVariable Long id) {
        return listingService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LANDLORD')")
    @SecurityRequirement(name = "bearerAuth")
    public ListingResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CreateListingRequest req,
            Authentication auth) {
        return listingService.update(id, req, auth.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        listingService.delete(id, auth.getName(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}