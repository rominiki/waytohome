package com.rominiki.waytohome.controller;
import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.service.ListingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/listings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final ListingService listingService;

    @GetMapping("/pending")
    public Page<ListingResponse> getPending(@PageableDefault(size = 10) Pageable pageable) {
        return listingService.getPending(pageable);
    }

    @PutMapping("/{id}/approve")
    public ListingResponse approve(@PathVariable Long id) {
        return listingService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public ListingResponse reject(@PathVariable Long id) {
        return listingService.reject(id);
    }
}