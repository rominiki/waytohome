package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.repository.ListingRepository;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    ListingRepository listingRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ListingService listingService;

    @Test
    void update_byNonOwner_throwsAccessDenied() {
        User owner = User.builder().email("owner@test.com").build();
        Listing listing = Listing.builder().owner(owner).build();
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(listing));
        var req = new CreateListingRequest(
                "t", "d", BigDecimal.TEN, "loc", 2, true, false);
        assertThatThrownBy(() ->
                listingService.update(1L, req, "someoneelse@test.com"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
