package com.rominiki.waytohome.integration.security;

import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@Transactional
class AuthorizationNegativePathTest extends BaseIntegrationTest {

    private String studentToken;
    private String landlordToken;
    private String adminToken;
    private Long testListingId;

    @BeforeEach
    void setUp() {
        // Create test users with different roles
        createTestUser("student@test.com", "password123", Role.STUDENT);
        createTestUser("landlord@test.com", "password123", Role.LANDLORD);
        createTestUser("admin@test.com", "password123", Role.ADMIN);

        // Authenticate all users to get valid JWT tokens
        studentToken = authenticateUser("student@test.com", "password123");
        landlordToken = authenticateUser("landlord@test.com", "password123");
        adminToken = authenticateUser("admin@test.com", "password123");

        // Create a test listing for approval tests
        CreateListingRequest listingRequest = new CreateListingRequest(
                "Test Listing for Authorization",
                "Test description",
                new BigDecimal("1000.00"),
                "Test City",
                2,
                true,
                false
        );
        ListingResponse createdListing = createTestListing(landlordToken, listingRequest);
        testListingId = createdListing.id();
    }

    @Test
    @DisplayName("POST /api/listings as STUDENT returns 403 Forbidden")
    void createListing_asStudent_returns403() {
        // Arrange
        CreateListingRequest request = new CreateListingRequest(
                "Student Listing",
                "Student attempted listing",
                new BigDecimal("800.00"),
                "Student City",
                1,
                false,
                true
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateListingRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/listings",
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("PATCH /api/admin/listings/{id}/approve as LANDLORD returns 403 Forbidden")
    void approveListing_asLandlord_returns403() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(landlordToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/listings/" + testListingId + "/approve",
                HttpMethod.PATCH,
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /api/admin/users as STUDENT returns 403 Forbidden")
    void getUsers_asStudent_returns403() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /api/admin/users as LANDLORD returns 403 Forbidden")
    void getUsers_asLandlord_returns403() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(landlordToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @ParameterizedTest
    @MethodSource("adminEndpointTestCases")
    @DisplayName("Property 3: All admin endpoints return 403 for non-admin users")
    void allAdminEndpoints_asNonAdminUser_return403(String endpoint, HttpMethod method, String role) {
        // Arrange - get the appropriate token based on role
        String token = role.equals("STUDENT") ? studentToken : landlordToken;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                method,
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode())
                .as("Endpoint %s with method %s should return 403 for role %s", endpoint, method, role)
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static Stream<Arguments> adminEndpointTestCases() {
        return Stream.of(
                // GET /api/admin/listings/pending - STUDENT
                Arguments.of("/api/admin/listings/pending", HttpMethod.GET, "STUDENT"),
                // GET /api/admin/listings/pending - LANDLORD
                Arguments.of("/api/admin/listings/pending", HttpMethod.GET, "LANDLORD"),
                // PUT /api/admin/listings/{id}/approve - STUDENT  
                Arguments.of("/api/admin/listings/1/approve", HttpMethod.PUT, "STUDENT"),
                // PUT /api/admin/listings/{id}/approve - LANDLORD
                Arguments.of("/api/admin/listings/1/approve", HttpMethod.PUT, "LANDLORD"),
                // PUT /api/admin/listings/{id}/reject - STUDENT
                Arguments.of("/api/admin/listings/1/reject", HttpMethod.PUT, "STUDENT"),
                // PUT /api/admin/listings/{id}/reject - LANDLORD
                Arguments.of("/api/admin/listings/1/reject", HttpMethod.PUT, "LANDLORD")
        );
    }
}
