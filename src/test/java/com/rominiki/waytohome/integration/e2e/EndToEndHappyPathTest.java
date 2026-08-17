package com.rominiki.waytohome.integration.e2e;

import com.rominiki.waytohome.dto.*;
import com.rominiki.waytohome.enums.ListingStatus;
import com.rominiki.waytohome.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.rominiki.waytohome.integration.base.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test that validates the complete happy path user journey
 * from registration through messaging between student and landlord.
 * 
 * Validates Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7
 */
@Transactional
class EndToEndHappyPathTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Complete user journey: registration -> listing creation -> approval -> favorite -> conversation -> messaging")
    void completeHappyPathJourney() {
        // Generate unique identifiers to avoid conflicts between test runs
        String unique = UUID.randomUUID().toString().substring(0, 8);
        
        String studentEmail = "student-" + unique + "@test.com";
        String landlordEmail = "landlord-" + unique + "@test.com";
        String adminEmail = "admin-" + unique + "@test.com";
        
        // Step 1: Register three users (STUDENT, LANDLORD, ADMIN)
        UserResponse student = createTestUser(studentEmail, "password123", Role.STUDENT);
        assertThat(student).isNotNull();
        assertThat(student.email()).isEqualTo(studentEmail);
        assertThat(student.role()).isEqualTo(Role.STUDENT);
        
        UserResponse landlord = createTestUser(landlordEmail, "password123", Role.LANDLORD);
        assertThat(landlord).isNotNull();
        assertThat(landlord.email()).isEqualTo(landlordEmail);
        assertThat(landlord.role()).isEqualTo(Role.LANDLORD);
        
        UserResponse admin = createTestUser(adminEmail, "password123", Role.ADMIN);
        assertThat(admin).isNotNull();
        assertThat(admin.email()).isEqualTo(adminEmail);
        assertThat(admin.role()).isEqualTo(Role.ADMIN);
        
        // Step 2: Authenticate all three users and store JWT tokens
        String studentToken = authenticateUser(studentEmail, "password123");
        assertThat(studentToken).isNotNull().isNotEmpty();
        
        String landlordToken = authenticateUser(landlordEmail, "password123");
        assertThat(landlordToken).isNotNull().isNotEmpty();
        
        String adminToken = authenticateUser(adminEmail, "password123");
        assertThat(adminToken).isNotNull().isNotEmpty();
        
        // Step 3: Landlord creates a listing (POST /api/listings)
        CreateListingRequest listingRequest = new CreateListingRequest(
                "Modern 2BR Apartment near Campus",
                "Spacious apartment with modern amenities, close to university campus",
                new BigDecimal("1200.00"),
                "Fulda",
                2,
                true,
                false
        );
        
        ListingResponse createdListing = createTestListing(landlordToken, listingRequest);
        assertThat(createdListing).isNotNull();
        assertThat(createdListing.id()).isNotNull();
        assertThat(createdListing.title()).isEqualTo("Modern 2BR Apartment near Campus");
        assertThat(createdListing.status()).isEqualTo(ListingStatus.PENDING);
        
        Long listingId = createdListing.id();
        
        // Step 4: Admin approves the listing (PUT /api/admin/listings/{id}/approve)
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<Void> adminEntity = new HttpEntity<>(adminHeaders);
        
        ResponseEntity<ListingResponse> approveResponse = restTemplate.exchange(
                "/api/admin/listings/" + listingId + "/approve",
                HttpMethod.PUT,
                adminEntity,
                ListingResponse.class
        );
        
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approveResponse.getBody()).isNotNull();
        assertThat(approveResponse.getBody().status()).isEqualTo(ListingStatus.APPROVED);
        
        // Step 5: Student favorites the listing (POST /api/favorites/{listingId})
        HttpHeaders studentHeaders = new HttpHeaders();
        studentHeaders.setBearerAuth(studentToken);
        HttpEntity<Void> favoriteEntity = new HttpEntity<>(studentHeaders);
        
        ResponseEntity<Void> favoriteResponse = restTemplate.exchange(
                "/api/favorites/" + listingId,
                HttpMethod.POST,
                favoriteEntity,
                Void.class
        );
        
        assertThat(favoriteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify the listing is in student's favorites
        ResponseEntity<ListingResponse[]> favoritesResponse = restTemplate.exchange(
                "/api/favorites",
                HttpMethod.GET,
                favoriteEntity,
                ListingResponse[].class
        );
        
        assertThat(favoritesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(favoritesResponse.getBody()).isNotNull();
        List<ListingResponse> favorites = List.of(favoritesResponse.getBody());
        assertThat(favorites).isNotEmpty();
        assertThat(favorites.stream().anyMatch(l -> l.id().equals(listingId))).isTrue();
        
        // Step 6: Student starts conversation (POST /api/conversations)
        StartConversationRequest conversationRequest = new StartConversationRequest(listingId);
        HttpHeaders conversationHeaders = new HttpHeaders();
        conversationHeaders.setBearerAuth(studentToken);
        conversationHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StartConversationRequest> conversationEntity = new HttpEntity<>(conversationRequest, conversationHeaders);
        
        ResponseEntity<ConversationResponse> conversationResponse = restTemplate.postForEntity(
                "/api/conversations",
                conversationEntity,
                ConversationResponse.class
        );
        
        assertThat(conversationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(conversationResponse.getBody()).isNotNull();
        assertThat(conversationResponse.getBody().listingId()).isEqualTo(listingId);
        
        Long conversationId = conversationResponse.getBody().id();
        
        // Step 7: Student sends message (POST /api/conversations/{id}/messages)
        ChatMessageRequest studentMessageRequest = new ChatMessageRequest(
                conversationId,
                "Hi! I'm interested in viewing this apartment. Is it still available?"
        );
        
        HttpHeaders studentMessageHeaders = new HttpHeaders();
        studentMessageHeaders.setBearerAuth(studentToken);
        studentMessageHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatMessageRequest> studentMessageEntity = new HttpEntity<>(studentMessageRequest, studentMessageHeaders);
        
        ResponseEntity<ChatMessageResponse> studentMessageResponse = restTemplate.postForEntity(
                "/api/conversations/" + conversationId + "/messages",
                studentMessageEntity,
                ChatMessageResponse.class
        );
        
        assertThat(studentMessageResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(studentMessageResponse.getBody()).isNotNull();
        assertThat(studentMessageResponse.getBody().content()).isEqualTo("Hi! I'm interested in viewing this apartment. Is it still available?");
        assertThat(studentMessageResponse.getBody().conversationId()).isEqualTo(conversationId);
        
        // Step 8: Landlord retrieves messages (GET /api/conversations/{id}/messages)
        HttpHeaders landlordHeaders = new HttpHeaders();
        landlordHeaders.setBearerAuth(landlordToken);
        HttpEntity<Void> landlordEntity = new HttpEntity<>(landlordHeaders);
        
        ResponseEntity<String> landlordMessagesResponse = restTemplate.exchange(
                "/api/conversations/" + conversationId + "/messages?page=0&size=20",
                HttpMethod.GET,
                landlordEntity,
                String.class
        );
        
        assertThat(landlordMessagesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(landlordMessagesResponse.getBody()).isNotNull();
        assertThat(landlordMessagesResponse.getBody()).contains("Hi! I'm interested in viewing this apartment. Is it still available?");
        
        // Step 9: Landlord replies to message (POST /api/conversations/{id}/messages)
        ChatMessageRequest landlordMessageRequest = new ChatMessageRequest(
                conversationId,
                "Yes, the apartment is still available! Would you like to schedule a viewing?"
        );
        
        HttpHeaders landlordMessageHeaders = new HttpHeaders();
        landlordMessageHeaders.setBearerAuth(landlordToken);
        landlordMessageHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChatMessageRequest> landlordMessageEntity = new HttpEntity<>(landlordMessageRequest, landlordMessageHeaders);
        
        ResponseEntity<ChatMessageResponse> landlordMessageResponse = restTemplate.postForEntity(
                "/api/conversations/" + conversationId + "/messages",
                landlordMessageEntity,
                ChatMessageResponse.class
        );
        
        assertThat(landlordMessageResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(landlordMessageResponse.getBody()).isNotNull();
        assertThat(landlordMessageResponse.getBody().content()).isEqualTo("Yes, the apartment is still available! Would you like to schedule a viewing?");
        assertThat(landlordMessageResponse.getBody().conversationId()).isEqualTo(conversationId);
        
        // Step 10: Student retrieves updated messages (GET /api/conversations/{id}/messages)
        ResponseEntity<String> studentMessagesResponse = restTemplate.exchange(
                "/api/conversations/" + conversationId + "/messages?page=0&size=20",
                HttpMethod.GET,
                favoriteEntity, // reuse student headers
                String.class
        );
        
        assertThat(studentMessagesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentMessagesResponse.getBody()).isNotNull();
        // Verify both messages are present
        assertThat(studentMessagesResponse.getBody()).contains("Hi! I'm interested in viewing this apartment. Is it still available?");
        assertThat(studentMessagesResponse.getBody()).contains("Yes, the apartment is still available! Would you like to schedule a viewing?");
        
        // Final verification: Both users can see the conversation in their conversation list
        ResponseEntity<ConversationResponse[]> studentConversationsResponse = restTemplate.exchange(
                "/api/conversations",
                HttpMethod.GET,
                favoriteEntity,
                ConversationResponse[].class
        );
        
        assertThat(studentConversationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(studentConversationsResponse.getBody()).isNotNull();
        assertThat(studentConversationsResponse.getBody()).hasSize(1);
        assertThat(studentConversationsResponse.getBody()[0].id()).isEqualTo(conversationId);
        
        ResponseEntity<ConversationResponse[]> landlordConversationsResponse = restTemplate.exchange(
                "/api/conversations",
                HttpMethod.GET,
                landlordEntity,
                ConversationResponse[].class
        );
        
        assertThat(landlordConversationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(landlordConversationsResponse.getBody()).isNotNull();
        assertThat(landlordConversationsResponse.getBody()).hasSize(1);
        assertThat(landlordConversationsResponse.getBody()[0].id()).isEqualTo(conversationId);
    }
}
