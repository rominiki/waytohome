package com.rominiki.waytohome.integration.resource;

import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import com.rominiki.waytohome.dto.ChatMessageRequest;
import com.rominiki.waytohome.dto.StartConversationRequest;
import com.rominiki.waytohome.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@Transactional
class ResourceNotFoundTest extends BaseIntegrationTest {

    private String studentToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Create test users with valid credentials and roles
        createTestUser("student@test.com", "password123", Role.STUDENT);
        createTestUser("admin@test.com", "password123", Role.ADMIN);

        // Authenticate users to obtain valid JWT tokens
        studentToken = authenticateUser("student@test.com", "password123");
        adminToken = authenticateUser("admin@test.com", "password123");
    }


    @Test
    @DisplayName("GET /api/listings/999999 returns 404 Not Found")
    void getListing_nonExistentId_returns404() {
        // Arrange
        Long nonExistentListingId = 999999L;

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/listings/" + nonExistentListingId,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    @DisplayName("PUT /api/admin/listings/999999/approve returns 404 Not Found")
    void approveListing_nonExistentId_returns404() {
        // Arrange
        Long nonExistentListingId = 999999L;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/listings/" + nonExistentListingId + "/approve",
                HttpMethod.PUT,
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    @DisplayName("POST /api/favorites/{listingId} with non-existent listing ID returns 404 Not Found")
    void createFavorite_nonExistentListingId_returns404() {
        // Arrange
        Long nonExistentListingId = 999999L;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/favorites/" + nonExistentListingId,
                HttpMethod.POST,
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    @DisplayName("POST /api/conversations with non-existent listing ID returns 404 Not Found")
    void startConversation_nonExistentListingId_returns404() {
        // Arrange
        Long nonExistentListingId = 999999L;
        StartConversationRequest request = new StartConversationRequest(nonExistentListingId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StartConversationRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/conversations",
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    @DisplayName("GET /api/conversations/999999/messages returns 404 Not Found")
    void getMessages_nonExistentConversationId_returns404() {
        // Arrange
        Long nonExistentConversationId = 999999L;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/conversations/" + nonExistentConversationId + "/messages",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Nested
    @DisplayName("Property-Based Tests: Resource Not Found Handling")
    class PropertyBasedResourceNotFoundTests {

        /**
         * Provides test data for parameterized tests.
         * Each argument contains: endpoint path template, HTTP method, requires auth token, description
         */
        private static Stream<Arguments> provideEndpointsAndIds() {
            return Stream.of(
                    // Listing endpoints
                    Arguments.of("/api/listings/{id}", HttpMethod.GET, false, "GET listing by ID"),
                    Arguments.of("/api/admin/listings/{id}/approve", HttpMethod.PUT, true, "Approve listing"),
                    
                    // Favorite endpoints
                    Arguments.of("/api/favorites/{listingId}", HttpMethod.POST, true, "Create favorite"),
                    
                    // Conversation endpoints
                    Arguments.of("/api/conversations/{id}/messages", HttpMethod.GET, true, "Get conversation messages")
            );
        }


        private static Stream<Long> provideNonExistentIds() {
            return Stream.of(
                    999999L,    // Large positive number (unlikely to exist)
                    0L,         // Zero (invalid ID)
                    -1L         // Negative number (invalid ID)
            );
        }


        @ParameterizedTest(name = "{3} with ID={0} returns 404")
        @MethodSource("provideGetEndpointsWithIds")
        @DisplayName("All GET endpoints with non-existent IDs return 404 Not Found")
        void allGetEndpoints_withNonExistentIds_return404(Long nonExistentId, String endpointTemplate, 
                                                          boolean requiresAuth, String description) {
            // Arrange
            String url = endpointTemplate.replace("{id}", String.valueOf(nonExistentId));
            HttpHeaders headers = new HttpHeaders();
            if (requiresAuth) {
                headers.setBearerAuth(studentToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Act
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Assert
            assertThat(response.getStatusCode())
                    .as("Endpoint %s with ID %d should return 404", endpointTemplate, nonExistentId)
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }


        @ParameterizedTest(name = "{3} with ID={0} returns 404")
        @MethodSource("providePostEndpointsWithIds")
        @DisplayName("All POST endpoints with non-existent IDs return 404 Not Found")
        void allPostEndpoints_withNonExistentIds_return404(Long nonExistentId, String endpointTemplate, 
                                                           String description) {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(studentToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response;

            // Act - Handle different POST endpoint patterns
            if (endpointTemplate.equals("/api/favorites/{listingId}")) {
                String url = endpointTemplate.replace("{listingId}", String.valueOf(nonExistentId));
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                
            } else if (endpointTemplate.equals("/api/conversations")) {
                StartConversationRequest request = new StartConversationRequest(nonExistentId);
                HttpEntity<StartConversationRequest> entity = new HttpEntity<>(request, headers);
                response = restTemplate.postForEntity("/api/conversations", entity, String.class);
                
            } else {
                throw new IllegalArgumentException("Unknown POST endpoint: " + endpointTemplate);
            }

            // Assert
            assertThat(response.getStatusCode())
                    .as("Endpoint %s with ID %d should return 404", endpointTemplate, nonExistentId)
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }


        @ParameterizedTest(name = "{3} with ID={0} returns 404")
        @MethodSource("providePutEndpointsWithIds")
        @DisplayName("All PUT/PATCH endpoints with non-existent IDs return 404 Not Found")
        void allPutEndpoints_withNonExistentIds_return404(Long nonExistentId, String endpointTemplate, 
                                                          String description) {
            // Arrange
            String url = endpointTemplate.replace("{id}", String.valueOf(nonExistentId));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Act
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            // Assert
            assertThat(response.getStatusCode())
                    .as("Endpoint %s with ID %d should return 404", endpointTemplate, nonExistentId)
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }


        private static Stream<Arguments> provideGetEndpointsWithIds() {
            return Stream.of(
                    // GET /api/listings/{id} - public endpoint
                    Arguments.of(999999L, "/api/listings/{id}", false, "GET listing"),
                    Arguments.of(0L, "/api/listings/{id}", false, "GET listing"),
                    Arguments.of(-1L, "/api/listings/{id}", false, "GET listing"),
                    
                    // GET /api/conversations/{id}/messages - requires auth
                    Arguments.of(999999L, "/api/conversations/{id}/messages", true, "GET conversation messages"),
                    Arguments.of(0L, "/api/conversations/{id}/messages", true, "GET conversation messages"),
                    Arguments.of(-1L, "/api/conversations/{id}/messages", true, "GET conversation messages")
            );
        }


        private static Stream<Arguments> providePostEndpointsWithIds() {
            return Stream.of(
                    // POST /api/favorites/{listingId}
                    Arguments.of(999999L, "/api/favorites/{listingId}", "Create favorite for listing"),
                    Arguments.of(0L, "/api/favorites/{listingId}", "Create favorite for listing"),
                    Arguments.of(-1L, "/api/favorites/{listingId}", "Create favorite for listing"),
                    
                    // POST /api/conversations (with listingId in body)
                    Arguments.of(999999L, "/api/conversations", "Start conversation for listing"),
                    Arguments.of(0L, "/api/conversations", "Start conversation for listing"),
                    Arguments.of(-1L, "/api/conversations", "Start conversation for listing")
            );
        }


        private static Stream<Arguments> providePutEndpointsWithIds() {
            return Stream.of(
                    // PUT /api/admin/listings/{id}/approve
                    Arguments.of(999999L, "/api/admin/listings/{id}/approve", "Approve listing"),
                    Arguments.of(0L, "/api/admin/listings/{id}/approve", "Approve listing"),
                    Arguments.of(-1L, "/api/admin/listings/{id}/approve", "Approve listing")
            );
        }
    }
}
