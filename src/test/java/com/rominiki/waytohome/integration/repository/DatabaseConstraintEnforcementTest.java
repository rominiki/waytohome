package com.rominiki.waytohome.integration.repository;

import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.ListingStatus;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import com.rominiki.waytohome.repository.ListingRepository;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Transactional
@DisplayName("Database Constraint Enforcement Tests")
class DatabaseConstraintEnforcementTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;
    
    @Autowired
    private com.rominiki.waytohome.repository.FavoriteRepository favoriteRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up existing data (order matters: delete dependent tables first)
        favoriteRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Unique constraint on email throws DataIntegrityViolationException")
    void uniqueConstraintOnEmail_throwsDataIntegrityViolationException() {
        // Arrange: Create first user with email
        User firstUser = User.builder()
                .email("duplicate@test.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("First User")
                .role(Role.STUDENT)
                .build();
        userRepository.saveAndFlush(firstUser);

        // Act & Assert: Attempt to create second user with same email
        User secondUser = User.builder()
                .email("duplicate@test.com")
                .password(passwordEncoder.encode("password456"))
                .fullName("Second User")
                .role(Role.LANDLORD)
                .build();

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(secondUser);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("unique", "constraint", "email");
                });
    }

    @Test
    @DisplayName("Foreign key constraint on listing.owner_id enforced")
    void foreignKeyConstraintOnListingOwnerId_enforced() {
        // Arrange: Create a listing with a non-existent user ID
        Listing listing = Listing.builder()
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("1000.00"))
                .location("Test City")
                .bedrooms(2)
                .petFriendly(false)
                .accessible(false)
                .status(ListingStatus.PENDING)
                .build();

        // Manually set owner to a detached user with non-existent ID
        User nonExistentUser = User.builder()
                .id(99999L)  // Non-existent ID
                .email("nonexistent@test.com")
                .password("password")
                .fullName("Non Existent User")
                .role(Role.LANDLORD)
                .build();
        listing.setOwner(nonExistentUser);

        // Act & Assert: Attempt to save listing with non-existent owner
        assertThatThrownBy(() -> {
            listingRepository.saveAndFlush(listing);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("foreign", "constraint", "owner");
                });
    }

    @Test
    @DisplayName("Not null constraint on User.email enforced")
    void notNullConstraintOnUserEmail_enforced() {
        // Arrange: Create user with null email
        User userWithNullEmail = User.builder()
                .email(null)  // Null email violates constraint
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .role(Role.STUDENT)
                .build();

        // Act & Assert: Attempt to save user with null email
        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(userWithNullEmail);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "email");
                });
    }

    @Test
    @DisplayName("Not null constraint on User.password enforced")
    void notNullConstraintOnUserPassword_enforced() {
        // Arrange: Create user with null password
        User userWithNullPassword = User.builder()
                .email("test@test.com")
                .password(null)  // Null password violates constraint
                .fullName("Test User")
                .role(Role.STUDENT)
                .build();

        // Act & Assert: Attempt to save user with null password
        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(userWithNullPassword);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "password");
                });
    }

    @Test
    @DisplayName("Not null constraint on User.fullName enforced")
    void notNullConstraintOnUserFullName_enforced() {
        // Arrange: Create user with null full name
        User userWithNullFullName = User.builder()
                .email("test@test.com")
                .password(passwordEncoder.encode("password123"))
                .fullName(null)  // Null full name violates constraint
                .role(Role.STUDENT)
                .build();

        // Act & Assert: Attempt to save user with null full name
        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(userWithNullFullName);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "full_name");
                });
    }

    @Test
    @DisplayName("Not null constraint on User.role enforced")
    void notNullConstraintOnUserRole_enforced() {
        // Arrange: Create user with null role
        User userWithNullRole = User.builder()
                .email("test@test.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test User")
                .role(null)  // Null role violates constraint
                .build();

        // Act & Assert: Attempt to save user with null role
        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(userWithNullRole);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "role");
                });
    }

    @Test
    @DisplayName("Not null constraint on Listing.title enforced")
    void notNullConstraintOnListingTitle_enforced() {
        // Arrange: Create owner user first
        User owner = createOwner();

        // Create listing with null title
        Listing listingWithNullTitle = Listing.builder()
                .title(null)  // Null title violates constraint
                .description("Test description")
                .price(new BigDecimal("1000.00"))
                .location("Test City")
                .bedrooms(2)
                .petFriendly(false)
                .accessible(false)
                .status(ListingStatus.PENDING)
                .owner(owner)
                .build();

        // Act & Assert: Attempt to save listing with null title
        assertThatThrownBy(() -> {
            listingRepository.saveAndFlush(listingWithNullTitle);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "title");
                });
    }

    @Test
    @DisplayName("Not null constraint on Listing.price enforced")
    void notNullConstraintOnListingPrice_enforced() {
        // Arrange: Create owner user first
        User owner = createOwner();

        // Create listing with null price
        Listing listingWithNullPrice = Listing.builder()
                .title("Test Listing")
                .description("Test description")
                .price(null)  // Null price violates constraint
                .location("Test City")
                .bedrooms(2)
                .petFriendly(false)
                .accessible(false)
                .status(ListingStatus.PENDING)
                .owner(owner)
                .build();

        // Act & Assert: Attempt to save listing with null price
        assertThatThrownBy(() -> {
            listingRepository.saveAndFlush(listingWithNullPrice);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "price");
                });
    }

    @Test
    @DisplayName("Not null constraint on Listing.location enforced")
    void notNullConstraintOnListingLocation_enforced() {
        // Arrange: Create owner user first
        User owner = createOwner();

        // Create listing with null location
        Listing listingWithNullLocation = Listing.builder()
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("1000.00"))
                .location(null)  // Null location violates constraint
                .bedrooms(2)
                .petFriendly(false)
                .accessible(false)
                .status(ListingStatus.PENDING)
                .owner(owner)
                .build();

        // Act & Assert: Attempt to save listing with null location
        assertThatThrownBy(() -> {
            listingRepository.saveAndFlush(listingWithNullLocation);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "location");
                });
    }

    @Test
    @DisplayName("Not null constraint on Listing.bedrooms enforced")
    void notNullConstraintOnListingBedrooms_enforced() {
        // Arrange: Create owner user first
        User owner = createOwner();

        // Create listing with null bedrooms
        Listing listingWithNullBedrooms = Listing.builder()
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("1000.00"))
                .location("Test City")
                .bedrooms(null)  // Null bedrooms violates constraint
                .petFriendly(false)
                .accessible(false)
                .status(ListingStatus.PENDING)
                .owner(owner)
                .build();

        // Act & Assert: Attempt to save listing with null bedrooms
        assertThatThrownBy(() -> {
            listingRepository.saveAndFlush(listingWithNullBedrooms);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "bedrooms");
                });
    }

    @Test
    @DisplayName("Not null constraint on Listing.status enforced")
    void notNullConstraintOnListingStatus_enforced() {
        // Arrange: Create owner user first
        User owner = createOwner();

        // Create listing with null status
        Listing listingWithNullStatus = Listing.builder()
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("1000.00"))
                .location("Test City")
                .bedrooms(2)
                .petFriendly(false)
                .accessible(false)
                .status(null)  // Null status violates constraint
                .owner(owner)
                .build();

        // Act & Assert: Attempt to save listing with null status
        assertThatThrownBy(() -> {
            listingRepository.saveAndFlush(listingWithNullStatus);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "status");
                });
    }

    @Test
    @DisplayName("Not null constraint on Listing.owner_id enforced")
    void notNullConstraintOnListingOwnerId_enforced() {
        // Arrange: Create listing with null owner
        Listing listingWithNullOwner = Listing.builder()
                .title("Test Listing")
                .description("Test description")
                .price(new BigDecimal("1000.00"))
                .location("Test City")
                .bedrooms(2)
                .petFriendly(false)
                .accessible(false)
                .status(ListingStatus.PENDING)
                .owner(null)  // Null owner violates constraint
                .build();

        // Act & Assert: Attempt to save listing with null owner
        assertThatThrownBy(() -> {
            listingRepository.saveAndFlush(listingWithNullOwner);
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage().toLowerCase();
                    assertThat(message).containsAnyOf("not-null", "null", "owner_id");
                });
    }

    @Test
    @DisplayName("Valid user creation succeeds with all required fields")
    void validUserCreation_succeedsWithAllRequiredFields() {
        // Arrange: Create user with all required fields
        User validUser = User.builder()
                .email("valid@test.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Valid User")
                .role(Role.STUDENT)
                .build();

        // Act: Save user
        User savedUser = userRepository.saveAndFlush(validUser);

        // Assert: User saved successfully
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("valid@test.com");
        assertThat(savedUser.getFullName()).isEqualTo("Valid User");
        assertThat(savedUser.getRole()).isEqualTo(Role.STUDENT);
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Valid listing creation succeeds with all required fields")
    void validListingCreation_succeedsWithAllRequiredFields() {
        // Arrange: Create owner and listing with all required fields
        User owner = createOwner();

        Listing validListing = Listing.builder()
                .title("Valid Listing")
                .description("Valid description")
                .price(new BigDecimal("1000.00"))
                .location("Valid City")
                .bedrooms(2)
                .petFriendly(true)
                .accessible(false)
                .status(ListingStatus.PENDING)
                .owner(owner)
                .build();

        // Act: Save listing
        Listing savedListing = listingRepository.saveAndFlush(validListing);

        // Assert: Listing saved successfully
        assertThat(savedListing.getId()).isNotNull();
        assertThat(savedListing.getTitle()).isEqualTo("Valid Listing");
        assertThat(savedListing.getPrice()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(savedListing.getLocation()).isEqualTo("Valid City");
        assertThat(savedListing.getBedrooms()).isEqualTo(2);
        assertThat(savedListing.getStatus()).isEqualTo(ListingStatus.PENDING);
        assertThat(savedListing.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(savedListing.getCreatedAt()).isNotNull();
    }

    // Helper method
    private User createOwner() {
        User owner = User.builder()
                .email("owner@test.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Listing Owner")
                .role(Role.LANDLORD)
                .build();
        return userRepository.saveAndFlush(owner);
    }
}
