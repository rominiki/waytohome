package com.rominiki.waytohome.repository;

import com.rominiki.waytohome.dto.ListingSearchCriteria;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.ListingStatus;
import com.rominiki.waytohome.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import static com.rominiki.waytohome.enums.ListingStatus.APPROVED;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ListingSpecificationTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void search_byPriceRange_returnsOnlyMatching() {

        User owner = userRepository.save(
                User.builder()
                        .email("a@a.com")
                        .role(Role.LANDLORD)
                        .password("x")
                        .fullName("A")
                        .build());

        listingRepository.save(makeListing(owner, 300, APPROVED));
        listingRepository.save(makeListing(owner, 600, APPROVED));
        listingRepository.save(makeListing(owner, 900, APPROVED));

        var criteria = new ListingSearchCriteria(
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(700),
                null,
                null,
                null
        );

        var results = listingRepository.findAll(
                ListingSpecification.build(criteria),
                Pageable.unpaged()
        );

        assertThat(results).hasSize(1);

        assertThat(results.getContent().get(0).getPrice())
                .isEqualTo(BigDecimal.valueOf(600));
    }

    @Test
    void search_excludesPendingListings() {

        User owner = userRepository.save(
                User.builder()
                        .email("owner@test.com")
                        .role(Role.LANDLORD)
                        .password("x")
                        .fullName("Owner")
                        .build());

        listingRepository.save(makeListing(owner, 500, ListingStatus.PENDING));

        listingRepository.save(makeListing(owner, 600, ListingStatus.APPROVED));

        var criteria = new ListingSearchCriteria(
                null,
                null,
                null,
                null,
                null
        );

        var results = listingRepository.findAll(
                ListingSpecification.build(criteria),
                Pageable.unpaged()
        );

        assertThat(results).hasSize(1);

        assertThat(results.getContent().get(0).getStatus()).isEqualTo(APPROVED);

        assertThat(results.getContent().get(0).getPrice()).isEqualTo(BigDecimal.valueOf(600));
    }

    private Listing makeListing(User owner, int price, ListingStatus status) {

        return Listing.builder()
                .title("Test Listing")
                .description("Test Description")
                .location("Fulda")
                .price(BigDecimal.valueOf(price))
                .bedrooms(2)
                .petFriendly(true)
                .accessible(false)
                .status(status)
                .owner(owner)
                .build();
    }
}