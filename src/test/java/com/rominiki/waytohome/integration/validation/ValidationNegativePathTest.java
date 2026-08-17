package com.rominiki.waytohome.integration.validation;

import com.rominiki.waytohome.dto.*;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import com.rominiki.waytohome.repository.FavoriteRepository;
import com.rominiki.waytohome.repository.ListingRepository;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validation Negative Path Tests")
class ValidationNegativePathTest extends BaseIntegrationTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;


    @Test
    @DisplayName("POST /api/auth/register with invalid email returns 400")
    void register_invalidEmail_returns400() {
        // Arrange
        RegisterRequest invalidEmailRequest = new RegisterRequest(
                "invalid-email-format",  // Invalid email
                "password123",
                "Test User",
                Role.STUDENT
        );

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                invalidEmailRequest,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/auth/register with empty password returns 400")
    void register_emptyPassword_returns400() {
        // Arrange
        RegisterRequest emptyPasswordRequest = new RegisterRequest(
                "student@test.com",
                "",  // Empty password
                "Test User",
                Role.STUDENT
        );

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                emptyPasswordRequest,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/auth/register with whitespace-only name returns 400")
    void register_whitespaceOnlyName_returns400() {
        // Arrange
        RegisterRequest whitespaceNameRequest = new RegisterRequest(
                "student@test.com",
                "password123",
                "   ",  // Whitespace-only name
                Role.STUDENT
        );

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                whitespaceNameRequest,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("POST /api/listings with missing required fields returns 400")
    void createListing_missingRequiredFields_returns400() {
        // Arrange - Create landlord and authenticate
        UserResponse landlord = createTestUser("landlord@test.com", "password123", Role.LANDLORD);
        String token = authenticateUser("landlord@test.com", "password123");

        // Create listing request with null required fields
        CreateListingRequest invalidRequest = new CreateListingRequest(
                null,  // Missing title
                "Description",
                null,  // Missing price
                "City",
                2,
                true,
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateListingRequest> entity = new HttpEntity<>(invalidRequest, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/listings",
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/listings with price < 0 returns 400")
    void createListing_negativePrice_returns400() {
        // Arrange - Create landlord and authenticate
        UserResponse landlord = createTestUser("landlord2@test.com", "password123", Role.LANDLORD);
        String token = authenticateUser("landlord2@test.com", "password123");

        // Create listing request with negative price
        CreateListingRequest invalidRequest = new CreateListingRequest(
                "Test Listing",
                "Description",
                new BigDecimal("-100.00"),  // Negative price
                "City",
                2,
                true,
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateListingRequest> entity = new HttpEntity<>(invalidRequest, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/listings",
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/listings with title exceeding 255 characters returns 400")
    void createListing_titleExceeds255Characters_returns400() {
        // Arrange - Create landlord and authenticate
        UserResponse landlord = createTestUser("landlord3@test.com", "password123", Role.LANDLORD);
        String token = authenticateUser("landlord3@test.com", "password123");

        // Create a title with 256 characters
        String longTitle = "A".repeat(256);

        CreateListingRequest invalidRequest = new CreateListingRequest(
                longTitle,  // Title exceeding 255 characters
                "Description",
                new BigDecimal("1000.00"),
                "City",
                2,
                true,
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateListingRequest> entity = new HttpEntity<>(invalidRequest, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/listings",
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("POST /api/conversations/{id}/messages with empty message returns 400")
    void sendMessage_emptyContent_returns400() {
        // Arrange - Create student, landlord, create and approve listing, start conversation
        UserResponse student = createTestUser("student@test.com", "password123", Role.STUDENT);
        String studentToken = authenticateUser("student@test.com", "password123");

        UserResponse landlord = createTestUser("landlord4@test.com", "password123", Role.LANDLORD);
        String landlordToken = authenticateUser("landlord4@test.com", "password123");

        UserResponse admin = createTestUser("admin@test.com", "password123", Role.ADMIN);
        String adminToken = authenticateUser("admin@test.com", "password123");

        // Create and approve listing
        ListingResponse listing = createApprovedListing(landlordToken, adminToken);

        // Start conversation
        StartConversationRequest conversationRequest = new StartConversationRequest(listing.id());
        HttpHeaders conversationHeaders = new HttpHeaders();
        conversationHeaders.setBearerAuth(studentToken);
        conversationHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StartConversationRequest> conversationEntity = new HttpEntity<>(conversationRequest, conversationHeaders);
        
        ResponseEntity<ConversationResponse> conversationResponse = restTemplate.postForEntity(
                "/api/conversations",
                conversationEntity,
                ConversationResponse.class
        );
        Long conversationId = conversationResponse.getBody().id();

        // Create message request with empty content
        ChatMessageRequest emptyMessageRequest = new ChatMessageRequest(
                conversationId,
                ""  // Empty message content
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatMessageRequest> entity = new HttpEntity<>(emptyMessageRequest, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/conversations/" + conversationId + "/messages",
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/conversations/{id}/messages with whitespace-only message returns 400")
    void sendMessage_whitespaceOnlyContent_returns400() {
        // Arrange - Create student, landlord, create and approve listing, start conversation
        UserResponse student = createTestUser("student2@test.com", "password123", Role.STUDENT);
        String studentToken = authenticateUser("student2@test.com", "password123");

        UserResponse landlord = createTestUser("landlord5@test.com", "password123", Role.LANDLORD);
        String landlordToken = authenticateUser("landlord5@test.com", "password123");

        UserResponse admin = createTestUser("admin2@test.com", "password123", Role.ADMIN);
        String adminToken = authenticateUser("admin2@test.com", "password123");

        // Create and approve listing
        ListingResponse listing = createApprovedListing(landlordToken, adminToken);

        // Start conversation
        StartConversationRequest conversationRequest = new StartConversationRequest(listing.id());
        HttpHeaders conversationHeaders = new HttpHeaders();
        conversationHeaders.setBearerAuth(studentToken);
        conversationHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StartConversationRequest> conversationEntity = new HttpEntity<>(conversationRequest, conversationHeaders);
        
        ResponseEntity<ConversationResponse> conversationResponse = restTemplate.postForEntity(
                "/api/conversations",
                conversationEntity,
                ConversationResponse.class
        );
        Long conversationId = conversationResponse.getBody().id();

        // Create message request with whitespace-only content
        ChatMessageRequest whitespaceMessageRequest = new ChatMessageRequest(
                conversationId,
                "   "  // Whitespace-only message content
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatMessageRequest> entity = new HttpEntity<>(whitespaceMessageRequest, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/conversations/" + conversationId + "/messages",
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Nested
    @DisplayName("Property 5: Validation Failure Responses")
    class ValidationFailureResponsesPropertyTests {

        @Nested
        @DisplayName("Registration Endpoint Validation")
        class RegistrationValidationTests {

            @ParameterizedTest(name = "{0}")
            @MethodSource("provideInvalidRegistrationRequests")
            @DisplayName("POST /api/auth/register with various invalid inputs returns 400")
            void register_withInvalidInputs_returns400(String testCase, RegisterRequest invalidRequest) {
                // Act
                ResponseEntity<String> response = restTemplate.postForEntity(
                        "/api/auth/register",
                        invalidRequest,
                        String.class
                );

                // Assert
                assertThat(response.getStatusCode())
                        .as("Test case: %s", testCase)
                        .isEqualTo(HttpStatus.BAD_REQUEST);
            }

            private static Stream<Arguments> provideInvalidRegistrationRequests() {
                return Stream.of(
                        // Invalid email formats
                        Arguments.of("Invalid email - missing @",
                                new RegisterRequest("invalidemail", "password123", "Test User", Role.STUDENT)),
                        Arguments.of("Invalid email - missing domain",
                                new RegisterRequest("test@", "password123", "Test User", Role.STUDENT)),
                        Arguments.of("Invalid email - no TLD",
                                new RegisterRequest("test@domain", "password123", "Test User", Role.STUDENT)),
                        Arguments.of("Invalid email - spaces",
                                new RegisterRequest("test @email.com", "password123", "Test User", Role.STUDENT)),

                        // Empty/blank required fields
                        Arguments.of("Empty email",
                                new RegisterRequest("", "password123", "Test User", Role.STUDENT)),
                        Arguments.of("Whitespace-only email",
                                new RegisterRequest("   ", "password123", "Test User", Role.STUDENT)),
                        Arguments.of("Empty password",
                                new RegisterRequest("test@email.com", "", "Test User", Role.STUDENT)),
                        Arguments.of("Whitespace-only password",
                                new RegisterRequest("test@email.com", "   ", "Test User", Role.STUDENT)),
                        Arguments.of("Empty full name",
                                new RegisterRequest("test@email.com", "password123", "", Role.STUDENT)),
                        Arguments.of("Whitespace-only full name",
                                new RegisterRequest("test@email.com", "password123", "   ", Role.STUDENT)),

                        // Password length constraints
                        Arguments.of("Password too short - 7 chars",
                                new RegisterRequest("test@email.com", "pass123", "Test User", Role.STUDENT)),
                        Arguments.of("Password too short - 1 char",
                                new RegisterRequest("test@email.com", "p", "Test User", Role.STUDENT)),

                        // Null required fields
                        Arguments.of("Null email",
                                new RegisterRequest(null, "password123", "Test User", Role.STUDENT)),
                        Arguments.of("Null password",
                                new RegisterRequest("test@email.com", null, "Test User", Role.STUDENT)),
                        Arguments.of("Null full name",
                                new RegisterRequest("test@email.com", "password123", null, Role.STUDENT)),
                        Arguments.of("Null role",
                                new RegisterRequest("test@email.com", "password123", "Test User", null))
                );
            }
        }

        @Nested
        @DisplayName("Listing Creation Endpoint Validation")
        class ListingCreationValidationTests {

            @ParameterizedTest(name = "{0}")
            @MethodSource("provideInvalidListingRequests")
            @DisplayName("POST /api/listings with various invalid inputs returns 400")
            void createListing_withInvalidInputs_returns400(String testCase, CreateListingRequest invalidRequest) {
                // Arrange - Create landlord and authenticate
                String uniqueEmail = "landlord-" + System.nanoTime() + "@test.com";
                UserResponse landlord = createTestUser(uniqueEmail, "password123", Role.LANDLORD);
                String token = authenticateUser(uniqueEmail, "password123");

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<CreateListingRequest> entity = new HttpEntity<>(invalidRequest, headers);

                // Act
                ResponseEntity<String> response = restTemplate.postForEntity(
                        "/api/listings",
                        entity,
                        String.class
                );

                // Assert
                assertThat(response.getStatusCode())
                        .as("Test case: %s", testCase)
                        .isEqualTo(HttpStatus.BAD_REQUEST);
            }

            private static Stream<Arguments> provideInvalidListingRequests() {
                return Stream.of(
                        // Empty/blank required fields
                        Arguments.of("Empty title",
                                new CreateListingRequest("", "Description", new BigDecimal("1000.00"), "City", 2, true, false)),
                        Arguments.of("Whitespace-only title",
                                new CreateListingRequest("   ", "Description", new BigDecimal("1000.00"), "City", 2, true, false)),
                        Arguments.of("Empty location",
                                new CreateListingRequest("Test Listing", "Description", new BigDecimal("1000.00"), "", 2, true, false)),
                        Arguments.of("Whitespace-only location",
                                new CreateListingRequest("Test Listing", "Description", new BigDecimal("1000.00"), "   ", 2, true, false)),

                        // Null required fields
                        Arguments.of("Null title",
                                new CreateListingRequest(null, "Description", new BigDecimal("1000.00"), "City", 2, true, false)),
                        Arguments.of("Null price",
                                new CreateListingRequest("Test Listing", "Description", null, "City", 2, true, false)),
                        Arguments.of("Null location",
                                new CreateListingRequest("Test Listing", "Description", new BigDecimal("1000.00"), null, 2, true, false)),
                        Arguments.of("Null bedrooms",
                                new CreateListingRequest("Test Listing", "Description", new BigDecimal("1000.00"), "City", null, true, false)),

                        // Invalid numeric values
                        Arguments.of("Negative price",
                                new CreateListingRequest("Test Listing", "Description", new BigDecimal("-100.00"), "City", 2, true, false)),
                        Arguments.of("Zero price",
                                new CreateListingRequest("Test Listing", "Description", new BigDecimal("0.00"), "City", 2, true, false)),
                        Arguments.of("Negative bedrooms",
                                new CreateListingRequest("Test Listing", "Description", new BigDecimal("1000.00"), "City", -1, true, false)),

                        // Length constraints
                        Arguments.of("Title exceeds 255 characters",
                                new CreateListingRequest("A".repeat(256), "Description", new BigDecimal("1000.00"), "City", 2, true, false)),
                        Arguments.of("Title at boundary - 256 characters",
                                new CreateListingRequest("T".repeat(256), "Description", new BigDecimal("1000.00"), "City", 2, true, false))
                );
            }
        }

        @Nested
        @DisplayName("Message Sending Endpoint Validation")
        class MessageSendingValidationTests {

            @ParameterizedTest(name = "{0}")
            @MethodSource("provideInvalidMessageRequests")
            @DisplayName("POST /api/conversations/{id}/messages with various invalid inputs returns 400")
            void sendMessage_withInvalidInputs_returns400(String testCase, String messageContent) {
                // Arrange - Create test setup with conversation
                String studentEmail = "student-" + System.nanoTime() + "@test.com";
                String landlordEmail = "landlord-" + System.nanoTime() + "@test.com";
                String adminEmail = "admin-" + System.nanoTime() + "@test.com";

                UserResponse student = createTestUser(studentEmail, "password123", Role.STUDENT);
                String studentToken = authenticateUser(studentEmail, "password123");

                UserResponse landlord = createTestUser(landlordEmail, "password123", Role.LANDLORD);
                String landlordToken = authenticateUser(landlordEmail, "password123");

                UserResponse admin = createTestUser(adminEmail, "password123", Role.ADMIN);
                String adminToken = authenticateUser(adminEmail, "password123");

                // Create and approve listing
                ListingResponse listing = createApprovedListing(landlordToken, adminToken);

                // Start conversation
                StartConversationRequest conversationRequest = new StartConversationRequest(listing.id());
                HttpHeaders conversationHeaders = new HttpHeaders();
                conversationHeaders.setBearerAuth(studentToken);
                conversationHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<StartConversationRequest> conversationEntity = new HttpEntity<>(conversationRequest, conversationHeaders);

                ResponseEntity<ConversationResponse> conversationResponse = restTemplate.postForEntity(
                        "/api/conversations",
                        conversationEntity,
                        ConversationResponse.class
                );
                Long conversationId = conversationResponse.getBody().id();

                // Create invalid message request
                ChatMessageRequest invalidMessageRequest = new ChatMessageRequest(conversationId, messageContent);

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(studentToken);
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<ChatMessageRequest> entity = new HttpEntity<>(invalidMessageRequest, headers);

                // Act
                ResponseEntity<String> response = restTemplate.postForEntity(
                        "/api/conversations/" + conversationId + "/messages",
                        entity,
                        String.class
                );

                // Assert
                assertThat(response.getStatusCode())
                        .as("Test case: %s", testCase)
                        .isEqualTo(HttpStatus.BAD_REQUEST);
            }

            private static Stream<Arguments> provideInvalidMessageRequests() {
                return Stream.of(
                        // Empty/blank content
                        Arguments.of("Empty message content", ""),
                        Arguments.of("Whitespace-only message content - spaces", "   "),
                        Arguments.of("Whitespace-only message content - tabs", "\t\t\t"),
                        Arguments.of("Whitespace-only message content - newlines", "\n\n\n"),
                        Arguments.of("Whitespace-only message content - mixed", "  \t\n  "),

                        // Length constraints
                        Arguments.of("Message exceeds 2000 characters", "A".repeat(2001)),
                        Arguments.of("Message at boundary - 2001 characters", "M".repeat(2001))
                );
            }
        }

        @Nested
        @DisplayName("Multiple Endpoints Validation Coverage")
        class MultipleEndpointsValidationTests {

            @ParameterizedTest(name = "Endpoint: {0}, Scenario: {1}")
            @MethodSource("provideEndpointValidationScenarios")
            @DisplayName("Various endpoints with invalid inputs return 400")
            void variousEndpoints_withInvalidInputs_return400(
                    String endpoint,
                    String scenario,
                    String method,
                    String requestBodyJson,
                    boolean requiresAuth,
                    Role requiredRole) {

                // Arrange
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                if (requiresAuth) {
                    String uniqueEmail = "user-" + System.nanoTime() + "@test.com";
                    createTestUser(uniqueEmail, "password123", requiredRole);
                    String token = authenticateUser(uniqueEmail, "password123");
                    headers.setBearerAuth(token);
                }

                HttpEntity<String> entity = new HttpEntity<>(requestBodyJson, headers);

                // Act
                ResponseEntity<String> response;
                if ("POST".equals(method)) {
                    response = restTemplate.postForEntity(endpoint, entity, String.class);
                } else if ("PUT".equals(method)) {
                    response = restTemplate.exchange(endpoint, HttpMethod.PUT, entity, String.class);
                } else if ("PATCH".equals(method)) {
                    response = restTemplate.exchange(endpoint, HttpMethod.PUT, entity, String.class);
                } else {
                    throw new IllegalArgumentException("Unsupported method: " + method);
                }

                // Assert
                assertThat(response.getStatusCode())
                        .as("Endpoint: %s, Scenario: %s", endpoint, scenario)
                        .isEqualTo(HttpStatus.BAD_REQUEST);
            }

            private static Stream<Arguments> provideEndpointValidationScenarios() {
                return Stream.of(
                        // Registration endpoint
                        Arguments.of("/api/auth/register", "Missing required field - email", "POST",
                                "{\"email\":null,\"password\":\"password123\",\"fullName\":\"Test\",\"role\":\"STUDENT\"}",
                                false, null),

                        // Listing creation endpoint
                        Arguments.of("/api/listings", "Missing title field", "POST",
                                "{\"title\":null,\"description\":\"Desc\",\"price\":1000,\"location\":\"City\",\"bedrooms\":2}",
                                true, Role.LANDLORD),
                        Arguments.of("/api/listings", "Invalid price value", "POST",
                                "{\"title\":\"Test\",\"description\":\"Desc\",\"price\":-500,\"location\":\"City\",\"bedrooms\":2}",
                                true, Role.LANDLORD)
                );
            }
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {


        @Test
        @DisplayName("GET /api/listings/search with minPrice=100 includes price=100 listings")
        void listingSearch_withMinPriceAtBoundary_includesBoundaryListings() {
            // Arrange - Create landlord and admin
            String landlordEmail = "landlord-search-" + System.nanoTime() + "@test.com";
            createTestUser(landlordEmail, "password123", Role.LANDLORD);
            String landlordToken = authenticateUser(landlordEmail, "password123");

            String adminEmail = "admin-search-" + System.nanoTime() + "@test.com";
            createTestUser(adminEmail, "password123", Role.ADMIN);
            String adminToken = authenticateUser(adminEmail, "password123");

            // Create listings with prices: 50, 100, 150
            CreateListingRequest listing50 = new CreateListingRequest(
                    "Listing at 50", "Description", new BigDecimal("50.00"), "City", 2, true, false);
            CreateListingRequest listing100 = new CreateListingRequest(
                    "Listing at 100", "Description", new BigDecimal("100.00"), "City", 2, true, false);
            CreateListingRequest listing150 = new CreateListingRequest(
                    "Listing at 150", "Description", new BigDecimal("150.00"), "City", 2, true, false);

            ListingResponse l50 = createTestListing(landlordToken, listing50);
            ListingResponse l100 = createTestListing(landlordToken, listing100);
            ListingResponse l150 = createTestListing(landlordToken, listing150);

            // Approve all listings
            HttpHeaders adminHeaders = new HttpHeaders();
            adminHeaders.setBearerAuth(adminToken);
            HttpEntity<Void> adminEntity = new HttpEntity<>(adminHeaders);

            restTemplate.exchange("/api/admin/listings/" + l50.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);
            restTemplate.exchange("/api/admin/listings/" + l100.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);
            restTemplate.exchange("/api/admin/listings/" + l150.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);

            // Act - Search with minPrice=100
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/listings/search?minPrice=100",
                    String.class
            );

            // Assert - Verify price=100 listing is included
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .contains("Listing at 100")
                    .contains("Listing at 150")
                    .doesNotContain("Listing at 50");
        }


        @Test
        @DisplayName("GET /api/listings/search with maxPrice=200 includes price=200 listings")
        void listingSearch_withMaxPriceAtBoundary_includesBoundaryListings() {
            // Arrange - Create landlord and admin
            String landlordEmail = "landlord-search2-" + System.nanoTime() + "@test.com";
            createTestUser(landlordEmail, "password123", Role.LANDLORD);
            String landlordToken = authenticateUser(landlordEmail, "password123");

            String adminEmail = "admin-search2-" + System.nanoTime() + "@test.com";
            createTestUser(adminEmail, "password123", Role.ADMIN);
            String adminToken = authenticateUser(adminEmail, "password123");

            // Create listings with prices: 150, 200, 250
            CreateListingRequest listing150 = new CreateListingRequest(
                    "Listing at 150", "Description", new BigDecimal("150.00"), "City", 2, true, false);
            CreateListingRequest listing200 = new CreateListingRequest(
                    "Listing at 200", "Description", new BigDecimal("200.00"), "City", 2, true, false);
            CreateListingRequest listing250 = new CreateListingRequest(
                    "Listing at 250", "Description", new BigDecimal("250.00"), "City", 2, true, false);

            ListingResponse l150 = createTestListing(landlordToken, listing150);
            ListingResponse l200 = createTestListing(landlordToken, listing200);
            ListingResponse l250 = createTestListing(landlordToken, listing250);

            // Approve all listings
            HttpHeaders adminHeaders = new HttpHeaders();
            adminHeaders.setBearerAuth(adminToken);
            HttpEntity<Void> adminEntity = new HttpEntity<>(adminHeaders);

            restTemplate.exchange("/api/admin/listings/" + l150.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);
            restTemplate.exchange("/api/admin/listings/" + l200.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);
            restTemplate.exchange("/api/admin/listings/" + l250.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);

            // Act - Search with maxPrice=200
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/listings/search?maxPrice=200",
                    String.class
            );

            // Assert - Verify price=200 listing is included
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .contains("Listing at 150")
                    .contains("Listing at 200")
                    .doesNotContain("Listing at 250");
        }


        @Test
        @DisplayName("GET /api/listings with page=0 returns first page")
        void listingSearch_withPageZero_returnsFirstPage() {
            // Arrange - Create landlord and admin
            String landlordEmail = "landlord-page-" + System.nanoTime() + "@test.com";
            createTestUser(landlordEmail, "password123", Role.LANDLORD);
            String landlordToken = authenticateUser(landlordEmail, "password123");

            String adminEmail = "admin-page-" + System.nanoTime() + "@test.com";
            createTestUser(adminEmail, "password123", Role.ADMIN);
            String adminToken = authenticateUser(adminEmail, "password123");

            // Create and approve 3 listings
            for (int i = 1; i <= 3; i++) {
                CreateListingRequest listingRequest = new CreateListingRequest(
                        "Page Test Listing " + i, "Description", new BigDecimal("100.00"), "City", 2, true, false);
                ListingResponse listing = createTestListing(landlordToken, listingRequest);

                HttpHeaders adminHeaders = new HttpHeaders();
                adminHeaders.setBearerAuth(adminToken);
                HttpEntity<Void> adminEntity = new HttpEntity<>(adminHeaders);
                restTemplate.exchange("/api/admin/listings/" + listing.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);
            }

            // Act - Request with page=0
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/listings?page=0&size=10",
                    String.class
            );

            // Assert - Verify successful response with first page
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"number\":0"); // Page number is 0
        }


        @Test
        @DisplayName("GET /api/listings with size=1 returns single result")
        void listingSearch_withSizeOne_returnsSingleResult() {
            // Arrange - Create landlord and admin
            String landlordEmail = "landlord-size-" + System.nanoTime() + "@test.com";
            createTestUser(landlordEmail, "password123", Role.LANDLORD);
            String landlordToken = authenticateUser(landlordEmail, "password123");

            String adminEmail = "admin-size-" + System.nanoTime() + "@test.com";
            createTestUser(adminEmail, "password123", Role.ADMIN);
            String adminToken = authenticateUser(adminEmail, "password123");

            // Create and approve 3 listings
            for (int i = 1; i <= 3; i++) {
                CreateListingRequest listingRequest = new CreateListingRequest(
                        "Size Test Listing " + i, "Description", new BigDecimal("100.00"), "City", 2, true, false);
                ListingResponse listing = createTestListing(landlordToken, listingRequest);

                HttpHeaders adminHeaders = new HttpHeaders();
                adminHeaders.setBearerAuth(adminToken);
                HttpEntity<Void> adminEntity = new HttpEntity<>(adminHeaders);
                restTemplate.exchange("/api/admin/listings/" + listing.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);
            }

            // Act - Request with size=1
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/listings?page=0&size=1",
                    String.class
            );

            // Assert - Verify response contains size=1
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"size\":1");
            assertThat(response.getBody()).contains("\"numberOfElements\":1");
        }


        @Test
        @DisplayName("GET /api/listings with very large page number handles gracefully")
        void listingSearch_withVeryLargePageNumber_handlesGracefully() {
            // Arrange - Create landlord and admin
            String landlordEmail = "landlord-largepage-" + System.nanoTime() + "@test.com";
            createTestUser(landlordEmail, "password123", Role.LANDLORD);
            String landlordToken = authenticateUser(landlordEmail, "password123");

            String adminEmail = "admin-largepage-" + System.nanoTime() + "@test.com";
            createTestUser(adminEmail, "password123", Role.ADMIN);
            String adminToken = authenticateUser(adminEmail, "password123");

            // Create and approve 1 listing
            CreateListingRequest listingRequest = new CreateListingRequest(
                    "Large Page Test Listing", "Description", new BigDecimal("100.00"), "City", 2, true, false);
            ListingResponse listing = createTestListing(landlordToken, listingRequest);

            HttpHeaders adminHeaders = new HttpHeaders();
            adminHeaders.setBearerAuth(adminToken);
            HttpEntity<Void> adminEntity = new HttpEntity<>(adminHeaders);
            restTemplate.exchange("/api/admin/listings/" + listing.id() + "/approve", HttpMethod.PUT, adminEntity, Void.class);

            // Act - Request with very large page number (999999)
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/listings?page=999999&size=10",
                    String.class
            );

            // Assert - Verify graceful handling (returns empty page or 200 OK)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("\"content\":[]"); // Empty content
            assertThat(response.getBody()).contains("\"numberOfElements\":0");
        }


        @Test
        @DisplayName("Two simultaneous favorite requests for same listing - only one succeeds")
        void concurrentFavoriteRequests_forSameListing_onlyOneSucceeds() throws InterruptedException {
            // Arrange - Create student, landlord, and admin users
            String studentEmail = "student-concurrent-" + System.nanoTime() + "@test.com";
            createTestUser(studentEmail, "password123", Role.STUDENT);
            String studentToken = authenticateUser(studentEmail, "password123");

            String landlordEmail = "landlord-concurrent-" + System.nanoTime() + "@test.com";
            createTestUser(landlordEmail, "password123", Role.LANDLORD);
            String landlordToken = authenticateUser(landlordEmail, "password123");

            String adminEmail = "admin-concurrent-" + System.nanoTime() + "@test.com";
            createTestUser(adminEmail, "password123", Role.ADMIN);
            String adminToken = authenticateUser(adminEmail, "password123");

            // Create and approve a listing
            ListingResponse listing = createApprovedListing(landlordToken, adminToken);

            // Prepare for concurrent execution
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // Thread 1: Attempt to favorite the listing
            Thread thread1 = new Thread(() -> {
                try {
                    latch.await(); // Wait for signal to start

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(studentToken);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    try {
                        ResponseEntity<Void> response = restTemplate.exchange(
                                "/api/favorites/" + listing.id(),
                                HttpMethod.POST,
                                entity,
                                Void.class
                        );

                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Any exception (409 or database constraint violation) counts as failure
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Thread 2: Attempt to favorite the same listing
            Thread thread2 = new Thread(() -> {
                try {
                    latch.await(); // Wait for signal to start

                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(studentToken);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    try {
                        ResponseEntity<Void> response = restTemplate.exchange(
                                "/api/favorites/" + listing.id(),
                                HttpMethod.POST,
                                entity,
                                Void.class
                        );

                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Any exception (409 or database constraint violation) counts as failure
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Start both threads
            thread1.start();
            thread2.start();

            // Release both threads simultaneously
            latch.countDown();

            // Wait for both threads to complete
            thread1.join();
            thread2.join();

            // Assert - One should succeed, one should fail (either 409 Conflict or constraint violation)
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failureCount.get()).isEqualTo(1);

            // Verify database has exactly one favorite record for this user and listing
            User student = userRepository.findByEmail(studentEmail).orElseThrow();
            Listing listingEntity = listingRepository.findById(listing.id()).orElseThrow();
            long favoriteCount = favoriteRepository.findByUser(student).stream()
                    .filter(fav -> fav.getListing().getId().equals(listingEntity.getId()))
                    .count();

            assertThat(favoriteCount).isEqualTo(1);
        }
    }
}

