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

/**
 * Integration tests for authorization negative path scenarios.
 * Validates that endpoints properly reject requests from authenticated users
 * who lack the required role or permissions (403 Forbidden).
 * 
 * These tests verify Property 2 (Authorization Enforcement) and Property 3 (Admin Endpoint Protection).
 * All users in these tests have valid JWT tokens but lack the required role for the operation.
 * 
 * Validates Requirements: 4.3, 4.4, 4.5, 4.6
 * Property 2: Authorization Enforcement
 * Property 3: Admin Endpoint Protection
 */
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

    /**
     * Test: STUDENT attempting POST /api/listings returns 403
     * Validates Requirement 4.4: Students cannot create listings
     */
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

    /**
     * Test: LANDLORD attempting PATCH /api/admin/listings/{id}/approve returns 403
     * Validates Requirement 4.5: Landlords cannot approve listings
     */
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

    /**
     * Test: STUDENT attempting GET /api/admin/users returns 403
     * Validates Requirement 4.6: Non-admin users cannot access admin endpoints
     */
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

    /**
     * Test: LANDLORD attempting GET /api/admin/users returns 403
     * Validates Requirement 4.6: Non-admin users cannot access admin endpoints
     */
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

    /**
     * Property 3: Admin Endpoint Protection
     * 
     * For any admin-only endpoint, when a non-admin user (STUDENT or LANDLORD) 
     * attempts to access it, the system SHALL return HTTP 403 Forbidden.
     * 
     * **Validates: Requirements 4.6**
     * 
     * This parameterized test covers all admin endpoints in AdminController:
     * - GET /api/admin/listings/pending
     * - PUT /api/admin/listings/{id}/approve
     * - PUT /api/admin/listings/{id}/reject
     * 
     * Each endpoint is tested with both STUDENT and LANDLORD roles to ensure
     * comprehensive protection of admin-only functionality.
     */
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

    /**
     * Provides test cases for Property 3: Admin Endpoint Protection
     * 
     * Returns a stream of arguments with:
     * - endpoint: The admin endpoint URL (using a placeholder ID for dynamic resources)
     * - method: The HTTP method
     * - role: The user role (for logging/debugging)
     */
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
