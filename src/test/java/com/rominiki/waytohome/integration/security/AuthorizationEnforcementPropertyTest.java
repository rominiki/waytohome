package com.rominiki.waytohome.integration.security;

import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@Transactional
class AuthorizationEnforcementPropertyTest extends BaseIntegrationTest {

    private static String studentToken;
    private static String landlordToken;
    private static String adminToken;
    private static Long testListingId;

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

        // Create a test listing for update/delete/approval tests
        CreateListingRequest listingRequest = new CreateListingRequest(
                "Test Listing",
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

    @Nested
    @DisplayName("Property 2: Authorization Enforcement - LANDLORD-only endpoints")
    class LandlordOnlyEndpointsTest {

        @ParameterizedTest(name = "{0} as {1} returns 403")
        @MethodSource("landlordOnlyEndpointsWithUnauthorizedRoles")
        @DisplayName("landlordOnlyEndpoint_withWrongRole_returns403Forbidden")
        void landlordOnlyEndpoint_withWrongRole_returns403Forbidden(
                String endpointDescription,
                Role unauthorizedRole,
                String unauthorizedToken,
                HttpMethod method,
                String url,
                Object requestBody) {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(unauthorizedToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

            // Act
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    method,
                    entity,
                    String.class
            );

            // Assert
            assertThat(response.getStatusCode())
                    .as("Endpoint %s should return 403 for role %s", endpointDescription, unauthorizedRole)
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        static Stream<Arguments> landlordOnlyEndpointsWithUnauthorizedRoles() {
            CreateListingRequest createRequest = new CreateListingRequest(
                    "Unauthorized Listing",
                    "Description",
                    new BigDecimal("800.00"),
                    "City",
                    1,
                    false,
                    true
            );

            CreateListingRequest updateRequest = new CreateListingRequest(
                    "Updated Listing",
                    "Updated description",
                    new BigDecimal("900.00"),
                    "Updated City",
                    2,
                    true,
                    false
            );

            return Stream.of(
                    // POST /api/listings - Create listing
                    Arguments.of("POST /api/listings", Role.STUDENT, getStudentToken(), 
                            HttpMethod.POST, "/api/listings", createRequest),
                    Arguments.of("POST /api/listings", Role.ADMIN, getAdminToken(), 
                            HttpMethod.POST, "/api/listings", createRequest),

                    // PUT /api/listings/{id} - Update listing
                    Arguments.of("PUT /api/listings/{id}", Role.STUDENT, getStudentToken(), 
                            HttpMethod.PUT, "/api/listings/" + getTestListingId(), updateRequest),
                    Arguments.of("PUT /api/listings/{id}", Role.ADMIN, getAdminToken(), 
                            HttpMethod.PUT, "/api/listings/" + getTestListingId(), updateRequest)
            );
        }
    }

    @Nested
    @DisplayName("Property 2: Authorization Enforcement - ADMIN-only endpoints")
    class AdminOnlyEndpointsTest {

        @ParameterizedTest(name = "{0} as {1} returns 403")
        @MethodSource("adminOnlyEndpointsWithUnauthorizedRoles")
        @DisplayName("adminOnlyEndpoint_withWrongRole_returns403Forbidden")
        void adminOnlyEndpoint_withWrongRole_returns403Forbidden(
                String endpointDescription,
                Role unauthorizedRole,
                String unauthorizedToken,
                HttpMethod method,
                String url) {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(unauthorizedToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Act
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    method,
                    entity,
                    String.class
            );

            // Assert
            assertThat(response.getStatusCode())
                    .as("Endpoint %s should return 403 for role %s", endpointDescription, unauthorizedRole)
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        static Stream<Arguments> adminOnlyEndpointsWithUnauthorizedRoles() {
            return Stream.of(
                    // GET /api/admin/listings/pending - Get pending listings
                    Arguments.of("GET /api/admin/listings/pending", Role.STUDENT, getStudentToken(), 
                            HttpMethod.GET, "/api/admin/listings/pending"),
                    Arguments.of("GET /api/admin/listings/pending", Role.LANDLORD, getLandlordToken(), 
                            HttpMethod.GET, "/api/admin/listings/pending"),

                    // PUT /api/admin/listings/{id}/approve - Approve listing
                    Arguments.of("PUT /api/admin/listings/{id}/approve", Role.STUDENT, getStudentToken(), 
                            HttpMethod.PUT, "/api/admin/listings/" + getTestListingId() + "/approve"),
                    Arguments.of("PUT /api/admin/listings/{id}/approve", Role.LANDLORD, getLandlordToken(), 
                            HttpMethod.PUT, "/api/admin/listings/" + getTestListingId() + "/approve"),

                    // PUT /api/admin/listings/{id}/reject - Reject listing
                    Arguments.of("PUT /api/admin/listings/{id}/reject", Role.STUDENT, getStudentToken(), 
                            HttpMethod.PUT, "/api/admin/listings/" + getTestListingId() + "/reject"),
                    Arguments.of("PUT /api/admin/listings/{id}/reject", Role.LANDLORD, getLandlordToken(), 
                            HttpMethod.PUT, "/api/admin/listings/" + getTestListingId() + "/reject")
            );
        }
    }

    @Nested
    @DisplayName("Property 2: Authorization Enforcement - Multi-role endpoints")
    class MultiRoleEndpointsTest {

        @ParameterizedTest(name = "DELETE /api/listings/id as {0} returns 403")
        @MethodSource("deleteEndpointWithStudentRole")
        @DisplayName("deleteEndpoint_asStudent_returns403Forbidden")
        void deleteEndpoint_asStudent_returns403Forbidden(
                Role unauthorizedRole,
                String unauthorizedToken) {
            // Arrange
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(unauthorizedToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Act
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/listings/" + testListingId,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            // Assert
            assertThat(response.getStatusCode())
                    .as("DELETE /api/listings/{id} should return 403 for STUDENT role")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        static Stream<Arguments> deleteEndpointWithStudentRole() {
            return Stream.of(
                    Arguments.of(Role.STUDENT, getStudentToken())
            );
        }
    }

    // Helper methods to provide tokens in static context for MethodSource
    private static String getStudentToken() {
        return studentToken;
    }

    private static String getLandlordToken() {
        return landlordToken;
    }

    private static String getAdminToken() {
        return adminToken;
    }

    private static Long getTestListingId() {
        return testListingId;
    }
}
