package com.rominiki.waytohome.integration.repository;

import com.rominiki.waytohome.dto.ListingSearchCriteria;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.ListingStatus;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import com.rominiki.waytohome.repository.ListingRepository;
import com.rominiki.waytohome.repository.ListingSpecification;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@Transactional
@DisplayName("Listing Repository Integration Tests")
class ListingRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.rominiki.waytohome.repository.FavoriteRepository favoriteRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User landlordUser;

    @BeforeEach
    void setUp() {
        // Clean up existing data (order matters: delete dependent tables first)
        favoriteRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        // Create a landlord user for test listings
        landlordUser = User.builder()
                .email("landlord@test.com")
                .password(passwordEncoder.encode("password"))
                .fullName("Test Landlord")
                .role(Role.LANDLORD)
                .build();
        landlordUser = userRepository.save(landlordUser);
    }

    @Test
    @DisplayName("findByStatus returns only listings with specified status")
    void findByStatus_returnsOnlyListingsWithSpecifiedStatus() {
        // Arrange: Create listings with different statuses
        Listing pendingListing = createListing("Pending Listing", ListingStatus.PENDING, "Warszawa", new BigDecimal("1000"), 2);
        Listing approvedListing1 = createListing("Approved Listing 1", ListingStatus.APPROVED, "Kraków", new BigDecimal("1500"), 3);
        Listing approvedListing2 = createListing("Approved Listing 2", ListingStatus.APPROVED, "Gdańsk", new BigDecimal("1200"), 2);
        Listing rejectedListing = createListing("Rejected Listing", ListingStatus.REJECTED, "Wrocław", new BigDecimal("800"), 1);

        listingRepository.saveAll(List.of(pendingListing, approvedListing1, approvedListing2, rejectedListing));

        Pageable pageable = PageRequest.of(0, 10);

        // Act: Query only APPROVED listings
        Page<Listing> approvedListings = listingRepository.findByStatus(ListingStatus.APPROVED, pageable);

        // Assert
        assertThat(approvedListings.getContent()).hasSize(2);
        assertThat(approvedListings.getContent())
                .extracting(Listing::getStatus)
                .containsOnly(ListingStatus.APPROVED);
        assertThat(approvedListings.getContent())
                .extracting(Listing::getTitle)
                .containsExactlyInAnyOrder("Approved Listing 1", "Approved Listing 2");
    }

    @Test
    @DisplayName("findByStatus with PENDING status returns correct results")
    void findByStatus_withPendingStatus_returnsCorrectResults() {
        // Arrange
        Listing pending1 = createListing("Pending 1", ListingStatus.PENDING, "Poznań", new BigDecimal("900"), 1);
        Listing pending2 = createListing("Pending 2", ListingStatus.PENDING, "Łódź", new BigDecimal("1100"), 2);
        Listing approved = createListing("Approved", ListingStatus.APPROVED, "Szczecin", new BigDecimal("1300"), 3);

        listingRepository.saveAll(List.of(pending1, pending2, approved));

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Listing> pendingListings = listingRepository.findByStatus(ListingStatus.PENDING, pageable);

        // Assert
        assertThat(pendingListings.getContent()).hasSize(2);
        assertThat(pendingListings.getContent())
                .extracting(Listing::getStatus)
                .containsOnly(ListingStatus.PENDING);
    }

    @Test
    @DisplayName("ListingSpecification filters by city correctly")
    void listingSpecification_filtersByCity_correctlyFiltersResults() {
        // Arrange: Create listings in different cities
        Listing listing1 = createListing("Listing in Warszawa", ListingStatus.APPROVED, "Warszawa", new BigDecimal("1000"), 2);
        Listing listing2 = createListing("Listing in warszawa lowercase", ListingStatus.APPROVED, "warszawa", new BigDecimal("1100"), 2);
        Listing listing3 = createListing("Listing in Kraków", ListingStatus.APPROVED, "Kraków", new BigDecimal("1200"), 3);
        Listing pending = createListing("Pending in Warszawa", ListingStatus.PENDING, "Warszawa", new BigDecimal("950"), 2);

        listingRepository.saveAll(List.of(listing1, listing2, listing3, pending));

        // Act: Search for listings in "Warszawa" (case-insensitive)
        ListingSearchCriteria criteria = new ListingSearchCriteria(null, null, "warszawa", null, null);
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert: Should return only APPROVED listings in Warszawa (case-insensitive)
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Listing::getLocation)
                .allMatch(location -> location.toLowerCase().contains("warszawa"));
        assertThat(results)
                .extracting(Listing::getStatus)
                .containsOnly(ListingStatus.APPROVED);
    }

    @Test
    @DisplayName("ListingSpecification filters by price range correctly")
    void listingSpecification_filtersByPriceRange_correctlyFiltersResults() {
        // Arrange: Create listings with different prices
        Listing listing1 = createListing("Cheap", ListingStatus.APPROVED, "Kraków", new BigDecimal("500"), 1);
        Listing listing2 = createListing("Medium 1", ListingStatus.APPROVED, "Kraków", new BigDecimal("1000"), 2);
        Listing listing3 = createListing("Medium 2", ListingStatus.APPROVED, "Kraków", new BigDecimal("1500"), 2);
        Listing listing4 = createListing("Expensive", ListingStatus.APPROVED, "Kraków", new BigDecimal("2500"), 3);

        listingRepository.saveAll(List.of(listing1, listing2, listing3, listing4));

        // Act: Search for listings between 1000 and 1500 (inclusive)
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                new BigDecimal("1000"), 
                new BigDecimal("1500"), 
                null, 
                null, 
                null
        );
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert: Should return listings with price 1000, 1500 (boundary values included)
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Listing::getPrice)
                .containsExactlyInAnyOrder(new BigDecimal("1000"), new BigDecimal("1500"));
        assertThat(results)
                .extracting(Listing::getTitle)
                .containsExactlyInAnyOrder("Medium 1", "Medium 2");
    }

    @Test
    @DisplayName("ListingSpecification filters by minimum price only")
    void listingSpecification_filtersByMinPrice_correctlyFiltersResults() {
        // Arrange
        Listing listing1 = createListing("Low Price", ListingStatus.APPROVED, "Gdańsk", new BigDecimal("800"), 1);
        Listing listing2 = createListing("High Price 1", ListingStatus.APPROVED, "Gdańsk", new BigDecimal("1200"), 2);
        Listing listing3 = createListing("High Price 2", ListingStatus.APPROVED, "Gdańsk", new BigDecimal("1500"), 3);

        listingRepository.saveAll(List.of(listing1, listing2, listing3));

        // Act: Search for listings with minPrice >= 1200
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                new BigDecimal("1200"), 
                null, 
                null, 
                null, 
                null
        );
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Listing::getPrice)
                .allMatch(price -> price.compareTo(new BigDecimal("1200")) >= 0);
    }

    @Test
    @DisplayName("ListingSpecification filters by maximum price only")
    void listingSpecification_filtersByMaxPrice_correctlyFiltersResults() {
        // Arrange
        Listing listing1 = createListing("Low Price 1", ListingStatus.APPROVED, "Wrocław", new BigDecimal("700"), 1);
        Listing listing2 = createListing("Low Price 2", ListingStatus.APPROVED, "Wrocław", new BigDecimal("900"), 2);
        Listing listing3 = createListing("High Price", ListingStatus.APPROVED, "Wrocław", new BigDecimal("1500"), 3);

        listingRepository.saveAll(List.of(listing1, listing2, listing3));

        // Act: Search for listings with maxPrice <= 900
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                null, 
                new BigDecimal("900"), 
                null, 
                null, 
                null
        );
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Listing::getPrice)
                .allMatch(price -> price.compareTo(new BigDecimal("900")) <= 0);
    }

    @Test
    @DisplayName("ListingSpecification filters by bedrooms correctly")
    void listingSpecification_filtersByBedrooms_correctlyFiltersResults() {
        // Arrange
        Listing listing1 = createListing("Studio", ListingStatus.APPROVED, "Poznań", new BigDecimal("800"), 1);
        Listing listing2 = createListing("2 Bedroom 1", ListingStatus.APPROVED, "Poznań", new BigDecimal("1200"), 2);
        Listing listing3 = createListing("2 Bedroom 2", ListingStatus.APPROVED, "Poznań", new BigDecimal("1300"), 2);
        Listing listing4 = createListing("3 Bedroom", ListingStatus.APPROVED, "Poznań", new BigDecimal("1600"), 3);

        listingRepository.saveAll(List.of(listing1, listing2, listing3, listing4));

        // Act: Search for 2-bedroom listings
        ListingSearchCriteria criteria = new ListingSearchCriteria(null, null, null, null, 2);
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Listing::getBedrooms)
                .containsOnly(2);
    }

    @Test
    @DisplayName("ListingSpecification filters by pet-friendly status correctly")
    void listingSpecification_filtersByPetFriendly_correctlyFiltersResults() {
        // Arrange
        Listing petFriendly1 = createListingWithPets("Pet Friendly 1", ListingStatus.APPROVED, "Łódź", new BigDecimal("1000"), 2, true);
        Listing petFriendly2 = createListingWithPets("Pet Friendly 2", ListingStatus.APPROVED, "Łódź", new BigDecimal("1100"), 3, true);
        Listing noPets = createListingWithPets("No Pets", ListingStatus.APPROVED, "Łódź", new BigDecimal("1200"), 2, false);

        listingRepository.saveAll(List.of(petFriendly1, petFriendly2, noPets));

        // Act: Search for pet-friendly listings
        ListingSearchCriteria criteria = new ListingSearchCriteria(null, null, null, true, null);
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Listing::getPetFriendly)
                .containsOnly(true);
    }

    @Test
    @DisplayName("ListingSpecification combines multiple criteria correctly")
    void listingSpecification_combinesMultipleCriteria_correctlyFiltersResults() {
        // Arrange: Create diverse listings
        Listing match = createListingWithPets(
                "Perfect Match", 
                ListingStatus.APPROVED, 
                "Warszawa", 
                new BigDecimal("1200"), 
                2, 
                true
        );
        
        Listing wrongCity = createListingWithPets(
                "Wrong City", 
                ListingStatus.APPROVED, 
                "Kraków", 
                new BigDecimal("1200"), 
                2, 
                true
        );
        
        Listing wrongPrice = createListingWithPets(
                "Too Expensive", 
                ListingStatus.APPROVED, 
                "Warszawa", 
                new BigDecimal("2000"), 
                2, 
                true
        );
        
        Listing wrongBedrooms = createListingWithPets(
                "Wrong Bedrooms", 
                ListingStatus.APPROVED, 
                "Warszawa", 
                new BigDecimal("1200"), 
                3, 
                true
        );
        
        Listing noPets = createListingWithPets(
                "No Pets", 
                ListingStatus.APPROVED, 
                "Warszawa", 
                new BigDecimal("1200"), 
                2, 
                false
        );

        listingRepository.saveAll(List.of(match, wrongCity, wrongPrice, wrongBedrooms, noPets));

        // Act: Search with multiple criteria
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                new BigDecimal("1000"),  // minPrice
                new BigDecimal("1500"),  // maxPrice
                "warszawa",              // location
                true,                    // petFriendly
                2                        // bedrooms
        );
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert: Only the perfect match should be returned
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Perfect Match");
        assertThat(results.get(0).getLocation().toLowerCase()).contains("warszawa");
        assertThat(results.get(0).getPrice()).isBetween(new BigDecimal("1000"), new BigDecimal("1500"));
        assertThat(results.get(0).getBedrooms()).isEqualTo(2);
        assertThat(results.get(0).getPetFriendly()).isTrue();
    }

    @Test
    @DisplayName("ListingSpecification with no criteria returns all approved listings")
    void listingSpecification_withNoCriteria_returnsAllApprovedListings() {
        // Arrange
        Listing approved1 = createListing("Approved 1", ListingStatus.APPROVED, "Gdańsk", new BigDecimal("1000"), 2);
        Listing approved2 = createListing("Approved 2", ListingStatus.APPROVED, "Wrocław", new BigDecimal("1500"), 3);
        Listing pending = createListing("Pending", ListingStatus.PENDING, "Kraków", new BigDecimal("1200"), 2);
        Listing rejected = createListing("Rejected", ListingStatus.REJECTED, "Poznań", new BigDecimal("900"), 1);

        listingRepository.saveAll(List.of(approved1, approved2, pending, rejected));

        // Act: Search with empty criteria
        ListingSearchCriteria criteria = new ListingSearchCriteria(null, null, null, null, null);
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert: Should return only APPROVED listings
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Listing::getStatus)
                .containsOnly(ListingStatus.APPROVED);
    }

    @Test
    @DisplayName("findByStatus with pagination returns correct page")
    void findByStatus_withPagination_returnsCorrectPage() {
        // Arrange: Create 5 approved listings
        for (int i = 1; i <= 5; i++) {
            Listing listing = createListing(
                    "Listing " + i, 
                    ListingStatus.APPROVED, 
                    "City " + i, 
                    new BigDecimal(1000 + i * 100), 
                    2
            );
            listingRepository.save(listing);
        }

        // Act: Request first page with 2 items per page
        Pageable firstPage = PageRequest.of(0, 2);
        Page<Listing> results = listingRepository.findByStatus(ListingStatus.APPROVED, firstPage);

        // Assert
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getTotalElements()).isEqualTo(5);
        assertThat(results.getTotalPages()).isEqualTo(3);
        assertThat(results.isFirst()).isTrue();
        assertThat(results.hasNext()).isTrue();
    }

    @Test
    @DisplayName("ListingSpecification returns empty list when no matches found")
    void listingSpecification_noMatches_returnsEmptyList() {
        // Arrange
        Listing listing = createListing("Only Listing", ListingStatus.APPROVED, "Warszawa", new BigDecimal("1000"), 2);
        listingRepository.save(listing);

        // Act: Search with criteria that won't match
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                new BigDecimal("5000"),  // Very high minimum price
                null, 
                null, 
                null, 
                null
        );
        Specification<Listing> spec = ListingSpecification.build(criteria);
        List<Listing> results = listingRepository.findAll(spec);

        // Assert
        assertThat(results).isEmpty();
    }

    // Helper methods

    private Listing createListing(String title, ListingStatus status, String location, BigDecimal price, Integer bedrooms) {
        return Listing.builder()
                .title(title)
                .description("Test description for " + title)
                .status(status)
                .location(location)
                .price(price)
                .bedrooms(bedrooms)
                .petFriendly(false)
                .accessible(false)
                .owner(landlordUser)
                .build();
    }

    private Listing createListingWithPets(String title, ListingStatus status, String location, 
                                          BigDecimal price, Integer bedrooms, Boolean petFriendly) {
        return Listing.builder()
                .title(title)
                .description("Test description for " + title)
                .status(status)
                .location(location)
                .price(price)
                .bedrooms(bedrooms)
                .petFriendly(petFriendly)
                .accessible(false)
                .owner(landlordUser)
                .build();
    }
}
