package com.rominiki.waytohome.integration.resource;

import com.rominiki.waytohome.dto.*;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("Conflict Negative Path Tests")
class ConflictNegativePathTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/auth/register with duplicate email returns 409")
    void register_duplicateEmail_returns409() {
        // Arrange - Create first user with specific email using unique ID to avoid test pollution
        String uniqueId = String.valueOf(System.currentTimeMillis()) + "-" + Thread.currentThread().getId();
        String email = "duplicate-" + uniqueId + "@test.com";
        RegisterRequest firstRequest = new RegisterRequest(
                email,
                "password123",
                "First User",
                Role.STUDENT
        );
        
        // Act - Register first user (should succeed)
        ResponseEntity<UserResponse> firstResponse = restTemplate.postForEntity(
                "/api/auth/register",
                firstRequest,
                UserResponse.class
        );
        
        // Assert first registration succeeds
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Arrange - Attempt to register second user with same email
        RegisterRequest duplicateRequest = new RegisterRequest(
                email,  // Same email
                "differentPassword456",
                "Second User",
                Role.LANDLORD  // Different role
        );
        
        // Act - Attempt duplicate registration
        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity(
                "/api/auth/register",
                duplicateRequest,
                String.class
        );
        
        // Assert - Duplicate registration returns 409 Conflict
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateResponse.getBody()).contains("Email already registered");
    }


    @Test
    @DisplayName("POST /api/favorites/{listingId} with duplicate favorite returns 409")
    void addFavorite_duplicateFavorite_returns409() {
        // Arrange - Create student user
        String studentEmail = "student-fav@test.com";
        createTestUser(studentEmail, "password123", Role.STUDENT);
        String studentToken = authenticateUser(studentEmail, "password123");
        
        // Arrange - Create landlord and admin for approved listing
        String landlordEmail = "landlord-fav@test.com";
        createTestUser(landlordEmail, "password123", Role.LANDLORD);
        String landlordToken = authenticateUser(landlordEmail, "password123");
        
        String adminEmail = "admin-fav@test.com";
        createTestUser(adminEmail, "password123", Role.ADMIN);
        String adminToken = authenticateUser(adminEmail, "password123");
        
        // Arrange - Create and approve a listing
        ListingResponse listing = createApprovedListing(landlordToken, adminToken);
        
        // Act - Add favorite for the first time (should succeed)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Void> firstFavoriteResponse = restTemplate.postForEntity(
                "/api/favorites/" + listing.id(),
                entity,
                Void.class
        );
        
        // Assert first favorite succeeds
        assertThat(firstFavoriteResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        // Act - Attempt to add the same favorite again
        ResponseEntity<String> duplicateFavoriteResponse = restTemplate.exchange(
                "/api/favorites/" + listing.id(),
                HttpMethod.POST,
                entity,
                String.class
        );
        
        // Assert - Duplicate favorite returns 409 Conflict
        assertThat(duplicateFavoriteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateFavoriteResponse.getBody()).contains("already favorited");
    }

    @Test
    @DisplayName("POST /api/conversations with duplicate conversation returns 409")
    void startConversation_duplicateConversation_returns409() {
        // Arrange - Create student user
        String studentEmail = "student-conv@test.com";
        createTestUser(studentEmail, "password123", Role.STUDENT);
        String studentToken = authenticateUser(studentEmail, "password123");
        
        // Arrange - Create landlord and admin for approved listing
        String landlordEmail = "landlord-conv@test.com";
        createTestUser(landlordEmail, "password123", Role.LANDLORD);
        String landlordToken = authenticateUser(landlordEmail, "password123");
        
        String adminEmail = "admin-conv@test.com";
        createTestUser(adminEmail, "password123", Role.ADMIN);
        String adminToken = authenticateUser(adminEmail, "password123");
        
        // Arrange - Create and approve a listing
        ListingResponse listing = createApprovedListing(landlordToken, adminToken);
        
        // Arrange - Create conversation request
        StartConversationRequest conversationRequest = new StartConversationRequest(listing.id());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StartConversationRequest> entity = new HttpEntity<>(conversationRequest, headers);
        
        // Act - Start conversation for the first time (should succeed)
        ResponseEntity<ConversationResponse> firstConversationResponse = restTemplate.postForEntity(
                "/api/conversations",
                entity,
                ConversationResponse.class
        );
        
        // Assert first conversation succeeds
        assertThat(firstConversationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(firstConversationResponse.getBody()).isNotNull();
        Long firstConversationId = firstConversationResponse.getBody().id();
        
        // Act - Attempt to start another conversation for the same listing
        ResponseEntity<String> duplicateConversationResponse = restTemplate.postForEntity(
                "/api/conversations",
                entity,
                String.class
        );
        
        // Assert - Duplicate conversation returns 409 Conflict
        assertThat(duplicateConversationResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateConversationResponse.getBody()).contains("Conversation already exists");
    }
}
