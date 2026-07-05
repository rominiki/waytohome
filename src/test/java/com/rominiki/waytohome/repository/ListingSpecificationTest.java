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
import java.util.UUID;
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
        String unique = UUID.randomUUID().toString().substring(0, 8);

        User owner = userRepository.save(
                User.builder()
                        .email("owner-price-" + unique + "@test.com")
                        .role(Role.LANDLORD)
                        .password("x")
                        .fullName("Owner")
                        .build()
        );

        listingRepository.save(makeListing(owner, "Cheap " + unique, 300, APPROVED));
        listingRepository.save(makeListing(owner, "Medium " + unique, 600, APPROVED));
        listingRepository.save(makeListing(owner, "Expensive " + unique, 900, APPROVED));

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

        assertThat(results.getContent())
                .extracting(Listing::getTitle)
                .contains("Medium " + unique);

        assertThat(results.getContent())
                .extracting(Listing::getTitle)
                .doesNotContain("Cheap " + unique, "Expensive " + unique);
    }

    @Test
    void search_excludesPendingListings() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        User owner = userRepository.save(
                User.builder()
                        .email("owner-status-" + unique + "@test.com")
                        .role(Role.LANDLORD)
                        .password("x")
                        .fullName("Owner")
                        .build()
        );

        Listing pending = listingRepository.save(
                makeListing(owner, "Pending " + unique, 500, ListingStatus.PENDING)
        );

        Listing approved = listingRepository.save(
                makeListing(owner, "Approved " + unique, 600, ListingStatus.APPROVED)
        );

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

        assertThat(results.getContent())
                .extracting(Listing::getId)
                .doesNotContain(pending.getId());

        assertThat(results.getContent())
                .extracting(Listing::getId)
                .contains(approved.getId());
    }

    private Listing makeListing(
            User owner,
            String title,
            int price,
            ListingStatus status
    ) {
        return Listing.builder()
                .title(title)
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