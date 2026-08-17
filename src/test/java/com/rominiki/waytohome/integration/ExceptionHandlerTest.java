package com.rominiki.waytohome.integration;

import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.dto.StartConversationRequest;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exception Handler Integration Tests")
@Transactional
class ExceptionHandlerTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("Specific Exception Scenario Tests")
    class SpecificExceptionTests {

        @Test
        @DisplayName("DuplicateEmailException returns 409 with meaningful message")
        void duplicateEmail_returns409WithMeaningfulMessage() {
        // Arrange: Create a user with a specific email
        String email = "duplicate@test.com";
        RegisterRequest firstRequest = new RegisterRequest(email, "password123", "First User", Role.STUDENT);
        restTemplate.postForEntity("/api/auth/register", firstRequest, Map.class);

        // Act: Attempt to register another user with the same email
        RegisterRequest duplicateRequest = new RegisterRequest(email, "password456", "Second User", Role.LANDLORD);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/register", duplicateRequest, Map.class);

        // Assert: Verify 409 Conflict status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Assert: Verify error response format includes message field
        Map<String, Object> errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody).containsKey("message");

        // Assert: Verify error message is descriptive (not just "Error occurred")
        String errorMessage = (String) errorBody.get("message");
        assertThat(errorMessage)
                .isNotBlank()
                .isNotEqualTo("Error occurred")
                .contains("Email already registered")
                .contains(email);
    }

    @Test
    @DisplayName("ResourceNotFoundException returns 404 with meaningful message")
    void resourceNotFound_returns404WithMeaningfulMessage() {
        // Arrange: Create and authenticate a user to get a valid token
        String email = "test@example.com";
        createTestUser(email, "password123", Role.STUDENT);
        String token = authenticateUser(email, "password123");

        // Create headers with authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act: Attempt to access a non-existent listing
        Long nonExistentId = 99999L;
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/listings/" + nonExistentId,
                HttpMethod.GET,
                entity,
                Map.class
        );

        // Assert: Verify 404 Not Found status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Assert: Verify error response format includes message field
        Map<String, Object> errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody).containsKey("message");

        // Assert: Verify error message is descriptive (not just "Error occurred")
        String errorMessage = (String) errorBody.get("message");
        assertThat(errorMessage)
                .isNotBlank()
                .isNotEqualTo("Error occurred")
                .contains("not found");
    }

    @Test
    @DisplayName("ResourceNotFoundException on favorite returns 404 with meaningful message")
    void favoriteNotFound_returns404WithMeaningfulMessage() {
        // Arrange: Create and authenticate a user
        String email = "student@test.com";
        createTestUser(email, "password123", Role.STUDENT);
        String token = authenticateUser(email, "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act: Attempt to delete a non-existent favorite
        Long nonExistentFavoriteId = 99999L;
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/favorites/" + nonExistentFavoriteId,
                HttpMethod.DELETE,
                entity,
                Map.class
        );

        // Assert: Verify 404 Not Found status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Assert: Verify error response format includes message field
        Map<String, Object> errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody).containsKey("message");

        // Assert: Verify error message is descriptive
        String errorMessage = (String) errorBody.get("message");
        assertThat(errorMessage)
                .isNotBlank()
                .isNotEqualTo("Error occurred")
                .contains("not found");
    }

    @Test
    @DisplayName("ResourceNotFoundException on conversation returns 404 with meaningful message")
    void conversationNotFound_returns404WithMeaningfulMessage() {
        // Arrange: Create and authenticate a user
        String email = "user@test.com";
        createTestUser(email, "password123", Role.STUDENT);
        String token = authenticateUser(email, "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act: Attempt to get messages from a non-existent conversation
        Long nonExistentConversationId = 99999L;
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/conversations/" + nonExistentConversationId + "/messages",
                HttpMethod.GET,
                entity,
                Map.class
        );

        // Assert: Verify 404 Not Found status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Assert: Verify error response format includes message field
        Map<String, Object> errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody).containsKey("message");

        // Assert: Verify error message is descriptive
        String errorMessage = (String) errorBody.get("message");
        assertThat(errorMessage)
                .isNotBlank()
                .isNotEqualTo("Error occurred")
                .contains("not found");
    }
}


    @Nested
    @DisplayName("Property 6: Exception Handler Correctness")
    class ExceptionHandlerCorrectnessPropertyTests {

        @Test
        @DisplayName("DuplicateEmailException - returns correct status and meaningful message")
        void duplicateEmailException_returnsCorrectStatusAndMessage() {
            // Arrange: Register first user with a truly unique email
            // Use only alphanumeric characters to satisfy email validation regex
            String uniqueId = String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId();
            String email = "propdupemail" + uniqueId + "@test.com";
            RegisterRequest firstRequest = new RegisterRequest(email, "password123", "First User", Role.STUDENT);
            ResponseEntity<Map> firstResponse = restTemplate.postForEntity("/api/auth/register", firstRequest, Map.class);
            
            // Verify first registration succeeded
            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // Act: Attempt duplicate registration with same email but different details
            RegisterRequest duplicateRequest = new RegisterRequest(email, "differentPass456", "Second User", Role.LANDLORD);
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/register", duplicateRequest, Map.class);

            // Assert: HTTP 409 Conflict
            assertThat(response.getStatusCode())
                    .as("DuplicateEmailException should return HTTP 409")
                    .isEqualTo(HttpStatus.CONFLICT);

            // Assert: Response body contains message field
            Map<String, Object> errorBody = response.getBody();
            assertThat(errorBody)
                    .isNotNull()
                    .containsKey("message");

            // Assert: Message is meaningful
            String errorMessage = (String) errorBody.get("message");
            assertThat(errorMessage)
                    .isNotBlank()
                    .isNotEqualTo("Error occurred")
                    .containsIgnoringCase("Email already registered");
        }

        @Test
        @DisplayName("DuplicateFavoriteException - returns correct status and meaningful message")
        void duplicateFavoriteException_returnsCorrectStatusAndMessage() {
            // Arrange: Create users and approved listing with truly unique emails
            // Use only alphanumeric characters to satisfy email validation regex
            String uniqueId = String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId();
            String studentEmail = "studentfav" + uniqueId + "@test.com";
            String landlordEmail = "landlordfav" + uniqueId + "@test.com";
            String adminEmail = "adminfav" + uniqueId + "@test.com";

            createTestUser(studentEmail, "password123", Role.STUDENT);
            createTestUser(landlordEmail, "password123", Role.LANDLORD);
            createTestUser(adminEmail, "password123", Role.ADMIN);

            String studentToken = authenticateUser(studentEmail, "password123");
            String landlordToken = authenticateUser(landlordEmail, "password123");
            String adminToken = authenticateUser(adminEmail, "password123");

            ListingResponse listing = createApprovedListing(landlordToken, adminToken);

            // First favorite - POST /api/favorites/{listingId}
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(studentToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> firstFavorite = restTemplate.postForEntity("/api/favorites/" + listing.id(), entity, Map.class);
            
            // Verify first favorite succeeded
            assertThat(firstFavorite.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // Act: Attempt duplicate favorite
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/favorites/" + listing.id(), entity, Map.class);

            // Assert: HTTP 409 Conflict
            assertThat(response.getStatusCode())
                    .as("DuplicateFavoriteException should return HTTP 409")
                    .isEqualTo(HttpStatus.CONFLICT);

            // Assert: Response body contains message field
            Map<String, Object> errorBody = response.getBody();
            assertThat(errorBody)
                    .isNotNull()
                    .containsKey("message");

            // Assert: Message is meaningful
            String errorMessage = (String) errorBody.get("message");
            assertThat(errorMessage)
                    .isNotBlank()
                    .isNotEqualTo("Error occurred")
                    .containsIgnoringCase("already favorited");
        }

        @Test
        @DisplayName("DuplicateConversationException - returns correct status and meaningful message")
        void duplicateConversationException_returnsCorrectStatusAndMessage() {
            // Arrange: Create users and approved listing with truly unique emails
            // Use only alphanumeric characters to satisfy email validation regex
            String uniqueId = String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId();
            String studentEmail = "studentconv" + uniqueId + "@test.com";
            String landlordEmail = "landlordconv" + uniqueId + "@test.com";
            String adminEmail = "adminconv" + uniqueId + "@test.com";

            createTestUser(studentEmail, "password123", Role.STUDENT);
            createTestUser(landlordEmail, "password123", Role.LANDLORD);
            createTestUser(adminEmail, "password123", Role.ADMIN);

            String studentToken = authenticateUser(studentEmail, "password123");
            String landlordToken = authenticateUser(landlordEmail, "password123");
            String adminToken = authenticateUser(adminEmail, "password123");

            ListingResponse listing = createApprovedListing(landlordToken, adminToken);

            // First conversation
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(studentToken);
            StartConversationRequest convRequest = new StartConversationRequest(listing.id());
            HttpEntity<StartConversationRequest> entity = new HttpEntity<>(convRequest, headers);
            ResponseEntity<Map> firstConv = restTemplate.postForEntity("/api/conversations", entity, Map.class);
            
            // Verify first conversation succeeded
            assertThat(firstConv.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // Act: Attempt duplicate conversation
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/conversations", entity, Map.class);

            // Assert: HTTP 409 Conflict
            assertThat(response.getStatusCode())
                    .as("DuplicateConversationException should return HTTP 409")
                    .isEqualTo(HttpStatus.CONFLICT);

            // Assert: Response body contains message field
            Map<String, Object> errorBody = response.getBody();
            assertThat(errorBody)
                    .isNotNull()
                    .containsKey("message");

            // Assert: Message is meaningful
            String errorMessage = (String) errorBody.get("message");
            assertThat(errorMessage)
                    .isNotBlank()
                    .isNotEqualTo("Error occurred")
                    .containsIgnoringCase("Conversation already exists");
        }

        @Test
        @DisplayName("ResourceNotFoundException (listing) - returns correct status and meaningful message")
        void resourceNotFoundExceptionListing_returnsCorrectStatusAndMessage() {
            // Act: Access non-existent listing (no auth needed for GET /api/listings/*)
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/listings/999999",
                    Map.class
            );

            // Assert: HTTP 404 Not Found
            assertThat(response.getStatusCode())
                    .as("ResourceNotFoundException should return HTTP 404")
                    .isEqualTo(HttpStatus.NOT_FOUND);

            // Assert: Response body contains message field
            Map<String, Object> errorBody = response.getBody();
            assertThat(errorBody)
                    .isNotNull()
                    .containsKey("message");

            // Assert: Message is meaningful
            String errorMessage = (String) errorBody.get("message");
            assertThat(errorMessage)
                    .isNotBlank()
                    .isNotEqualTo("Error occurred")
                    .containsIgnoringCase("not found");
        }

        @Test
        @DisplayName("ResourceNotFoundException (favorite) - returns correct status and meaningful message")
        void resourceNotFoundExceptionFavorite_returnsCorrectStatusAndMessage() {
            // Arrange: Create student user and authenticate
            String uniqueId = String.valueOf(System.currentTimeMillis()) + "-" + Thread.currentThread().getId();
            String email = "user-fav-notfound-" + uniqueId + "@test.com";
            createTestUser(email, "pass123", Role.STUDENT);
            String token = authenticateUser(email, "pass123");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Act: Delete non-existent favorite (use a listingId that doesn't exist as favorite)
            // Since DELETE /api/favorites/{listingId} removes by listingId, not favoriteId
            // Use a non-existent listing ID
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/favorites/999999",
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            // Assert: HTTP 404 Not Found
            assertThat(response.getStatusCode())
                    .as("ResourceNotFoundException should return HTTP 404")
                    .isEqualTo(HttpStatus.NOT_FOUND);

            // Assert: Response body contains message field
            Map<String, Object> errorBody = response.getBody();
            assertThat(errorBody)
                    .isNotNull()
                    .containsKey("message");

            // Assert: Message is meaningful
            String errorMessage = (String) errorBody.get("message");
            assertThat(errorMessage)
                    .isNotBlank()
                    .isNotEqualTo("Error occurred")
                    .containsIgnoringCase("not found");
        }

        @Test
        @DisplayName("ResourceNotFoundException (conversation) - returns correct status and meaningful message")
        void resourceNotFoundExceptionConversation_returnsCorrectStatusAndMessage() {
            // Arrange: Create student user and authenticate
            String uniqueId = String.valueOf(System.currentTimeMillis()) + "-" + Thread.currentThread().getId();
            String email = "user-conv-notfound-" + uniqueId + "@test.com";
            createTestUser(email, "pass123", Role.STUDENT);
            String token = authenticateUser(email, "pass123");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Act: Access messages from non-existent conversation
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/conversations/999999/messages",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // Assert: HTTP 404 Not Found
            assertThat(response.getStatusCode())
                    .as("ResourceNotFoundException should return HTTP 404")
                    .isEqualTo(HttpStatus.NOT_FOUND);

            // Assert: Response body contains message field
            Map<String, Object> errorBody = response.getBody();
            assertThat(errorBody)
                    .isNotNull()
                    .containsKey("message");

            // Assert: Message is meaningful
            String errorMessage = (String) errorBody.get("message");
            assertThat(errorMessage)
                    .isNotBlank()
                    .isNotEqualTo("Error occurred")
                    .containsIgnoringCase("not found");
        }
    }
}
