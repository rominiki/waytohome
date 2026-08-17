package com.rominiki.waytohome.integration.validation;

import com.rominiki.waytohome.dto.*;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests validating input validation and error handling.
 * Tests that invalid inputs return HTTP 400 Bad Request.
 * 
 * Validates Requirements: 5.2, 6.1, 6.2
 */
@DisplayName("Validation Negative Path Tests")
class ValidationNegativePathTest extends BaseIntegrationTest {

    // ============================================
    // Auth Registration Validation Tests
    // ============================================

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

    // ============================================
    // Listing Creation Validation Tests
    // ============================================

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

    // ============================================
    // Conversation Message Validation Tests
    // ============================================

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

    // ============================================
    // Property 5: Validation Failure Responses
    // Property-Based Tests
    // ============================================

    /**
     * **Validates: Requirements 5.2, 6.1, 6.2**
     * Property 5: Validation Failure Responses
     * 
     * For any endpoint with input validation, when a request contains invalid data
     * (empty/whitespace required fields, exceeded length constraints, invalid formats),
     * the system SHALL return HTTP 400 Bad Request with validation error details.
     */
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
                    response = restTemplate.exchange(endpoint, HttpMethod.PATCH, entity, String.class);
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
                        Arguments.of("/api/auth/register", "Invalid JSON structure", "POST",
                                "{\"email\":\"test@test.com\",\"password\":}",
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
}
