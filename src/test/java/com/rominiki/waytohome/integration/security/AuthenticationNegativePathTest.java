package com.rominiki.waytohome.integration.security;

import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.dto.StartConversationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.rominiki.waytohome.integration.base.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class AuthenticationNegativePathTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/listings without token returns 403")
    void createListing_withoutToken_returns403() {
        // Arrange
        CreateListingRequest request = new CreateListingRequest(
                "Test Listing",
                "Test description",
                new BigDecimal("1000.00"),
                "Test City",
                2,
                true,
                false
        );
        
        // Act - make request without Authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateListingRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/listings",
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /api/favorites without token returns 403")
    void getFavorites_withoutToken_returns403() {
        // Act - make request without Authorization header
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/favorites",
                HttpMethod.GET,
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/conversations without token returns 403")
    void createConversation_withoutToken_returns403() {
        // Arrange
        StartConversationRequest request = new StartConversationRequest(1L);
        
        // Act - make request without Authorization header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StartConversationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/conversations",
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/listings with invalid token returns 403")
    void createListing_withInvalidToken_returns403() {
        // Arrange
        CreateListingRequest request = new CreateListingRequest(
                "Test Listing",
                "Test description",
                new BigDecimal("1000.00"),
                "Test City",
                2,
                true,
                false
        );
        
        String invalidToken = "invalid.jwt.token";
        
        // Act - make request with invalid token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(invalidToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateListingRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/listings",
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /api/favorites with invalid token returns 403")
    void getFavorites_withInvalidToken_returns403() {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        
        // Act - make request with invalid token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(invalidToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/favorites",
                HttpMethod.GET,
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/conversations with invalid token returns 403")
    void createConversation_withInvalidToken_returns403() {
        // Arrange
        StartConversationRequest request = new StartConversationRequest(1L);
        String invalidToken = "invalid.jwt.token";
        
        // Act - make request with invalid token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(invalidToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StartConversationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/conversations",
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/listings with malformed token returns 403")
    void createListing_withMalformedToken_returns403() {
        // Arrange
        CreateListingRequest request = new CreateListingRequest(
                "Test Listing",
                "Test description",
                new BigDecimal("1000.00"),
                "Test City",
                2,
                true,
                false
        );
        
        String malformedToken = "malformed-token-without-proper-structure";
        
        // Act - make request with malformed token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(malformedToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateListingRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/listings",
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /api/favorites with malformed token returns 403")
    void getFavorites_withMalformedToken_returns403() {
        // Arrange
        String malformedToken = "not.a.valid.jwt";
        
        // Act - make request with malformed token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(malformedToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/favorites",
                HttpMethod.GET,
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/conversations with malformed token returns 403")
    void createConversation_withMalformedToken_returns403() {
        // Arrange
        StartConversationRequest request = new StartConversationRequest(1L);
        String malformedToken = "totally-not-a-jwt-token";
        
        // Act - make request with malformed token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(malformedToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StartConversationRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/conversations",
                entity,
                String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }


    @Nested
    @DisplayName("Property 1: Authentication Failure Consistency")
    class PropertyBasedAuthenticationTests {


        private static Stream<Arguments> protectedEndpointsWithoutToken() {
            return Stream.of(
                    // Listing endpoints
                    Arguments.of(HttpMethod.POST, "/api/listings", createListingJson()),
                    Arguments.of(HttpMethod.PUT, "/api/listings/1", createListingJson()),
                    Arguments.of(HttpMethod.DELETE, "/api/listings/1", null),
                    
                    // Favorite endpoints
                    Arguments.of(HttpMethod.GET, "/api/favorites", null),
                    Arguments.of(HttpMethod.POST, "/api/favorites/1", null),
                    Arguments.of(HttpMethod.DELETE, "/api/favorites/1", null),
                    
                    // Conversation endpoints
                    Arguments.of(HttpMethod.POST, "/api/conversations", createConversationJson()),
                    Arguments.of(HttpMethod.GET, "/api/conversations", null),
                    Arguments.of(HttpMethod.GET, "/api/conversations/1/messages", null),
                    Arguments.of(HttpMethod.PUT, "/api/conversations/1/read", null),
                    Arguments.of(HttpMethod.POST, "/api/conversations/1/messages", createMessageJson())
            );
        }


        private static Stream<Arguments> protectedEndpointsWithInvalidToken() {
            return Stream.of(
                    // Listing endpoints
                    Arguments.of(HttpMethod.POST, "/api/listings", createListingJson(), "invalid.jwt.token"),
                    Arguments.of(HttpMethod.PUT, "/api/listings/1", createListingJson(), "malformed-token"),
                    Arguments.of(HttpMethod.DELETE, "/api/listings/1", null, "not.a.valid.jwt"),
                    
                    // Favorite endpoints
                    Arguments.of(HttpMethod.GET, "/api/favorites", null, "invalid.jwt.token"),
                    Arguments.of(HttpMethod.POST, "/api/favorites/1", null, "malformed-token"),
                    Arguments.of(HttpMethod.DELETE, "/api/favorites/1", null, "not.a.valid.jwt"),
                    
                    // Conversation endpoints
                    Arguments.of(HttpMethod.POST, "/api/conversations", createConversationJson(), "invalid.jwt.token"),
                    Arguments.of(HttpMethod.GET, "/api/conversations", null, "malformed-token"),
                    Arguments.of(HttpMethod.GET, "/api/conversations/1/messages", null, "not.a.valid.jwt"),
                    Arguments.of(HttpMethod.PUT, "/api/conversations/1/read", null, "invalid.jwt.token"),
                    Arguments.of(HttpMethod.POST, "/api/conversations/1/messages", createMessageJson(), "malformed-token")
            );
        }

        @ParameterizedTest(name = "{0} {1} without token returns 403")
        @MethodSource("protectedEndpointsWithoutToken")
        @DisplayName("All protected endpoints without token return 403 Forbidden")
        void allProtectedEndpoints_withoutToken_return403(HttpMethod method, String uri, String body) {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // Act - make request without Authorization header
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    method,
                    entity,
                    String.class
            );

            // Assert - Spring Security returns 403 for unauthorized requests
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @ParameterizedTest(name = "{0} {1} with invalid token returns 403")
        @MethodSource("protectedEndpointsWithInvalidToken")
        @DisplayName("All protected endpoints with invalid token return 403 Forbidden")
        void allProtectedEndpoints_withInvalidToken_return403(HttpMethod method, String uri, String body, String invalidToken) {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(invalidToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // Act - make request with invalid token
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    method,
                    entity,
                    String.class
            );

            // Assert - Spring Security returns 403 for invalid/malformed tokens
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        // Helper methods to create JSON request bodies
        private static String createListingJson() {
            return """
                {
                    "title": "Test Listing",
                    "description": "Test description",
                    "price": 1000.00,
                    "location": "Test City",
                    "bedrooms": 2,
                    "petFriendly": true,
                    "furnished": false
                }
                """;
        }

        private static String createConversationJson() {
            return """
                {
                    "listingId": 1
                }
                """;
        }

        private static String createMessageJson() {
            return """
                {
                    "conversationId": 1,
                    "content": "Test message"
                }
                """;
        }
    }
}
