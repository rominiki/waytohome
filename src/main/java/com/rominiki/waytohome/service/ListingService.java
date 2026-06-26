package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.*;
import com.rominiki.waytohome.entity.*;
import com.rominiki.waytohome.enums.ListingStatus;
import com.rominiki.waytohome.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.rominiki.waytohome.repository.ListingSpecification;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingResponse create(CreateListingRequest req, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow();
        Listing listing = Listing.builder()
                .title(req.title()).description(req.description())
                .price(req.price()).location(req.location())
                .bedrooms(req.bedrooms())
                .petFriendly(req.petFriendly()).accessible(req.accessible())
                .status(ListingStatus.PENDING)
                .owner(owner)
                .build();
        return ListingResponse.from(listingRepository.save(listing));
    }

    public Page<ListingResponse> getApproved(Pageable pageable) {
        return listingRepository
                .findByStatus(ListingStatus.APPROVED, pageable)
                .map(ListingResponse::from);
    }

    public ListingResponse getById(Long id) {
        return ListingResponse.from(
                listingRepository.findById(id).orElseThrow());
    }

    public ListingResponse update(Long id, CreateListingRequest req, String requesterEmail) {
        Listing listing = listingRepository.findById(id).orElseThrow();
        if (!listing.getOwner().getEmail().equals(requesterEmail)) {
            throw new AccessDeniedException("You do not own this listing");
        }
        listing.updateFrom(req);
        return ListingResponse.from(listingRepository.save(listing));
    }

    public void delete(Long id, String requesterEmail, boolean isAdmin) {
        Listing listing = listingRepository.findById(id).orElseThrow();
        boolean isOwner = listing.getOwner().getEmail().equals(requesterEmail);
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Not allowed to delete this listing");
        }
        listingRepository.delete(listing);
    }

    public Page<ListingResponse> search(ListingSearchCriteria criteria, Pageable pageable) {
        var spec = ListingSpecification.build(criteria);
        return listingRepository.findAll(spec, pageable).map(ListingResponse::from);
    }

    public Page<ListingResponse> getPending(Pageable pageable) {
        return listingRepository
                .findByStatus(ListingStatus.PENDING, pageable)
                .map(ListingResponse::from);
    }

    @Transactional
    public ListingResponse approve(Long id) {
        Listing listing = listingRepository.findById(id).orElseThrow();
        listing.approve();
        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional
    public ListingResponse reject(Long id) {
        Listing listing = listingRepository.findById(id).orElseThrow();
        listing.setStatus(ListingStatus.REJECTED);
        return ListingResponse.from(listingRepository.save(listing));
    }
}