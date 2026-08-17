package com.rominiki.waytohome.integration.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rominiki.waytohome.dto.*;
import com.rominiki.waytohome.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import java.math.BigDecimal;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    // Singleton container shared across ALL test classes
    private static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;


    protected UserResponse createTestUser(String email, String password, Role role) {
        RegisterRequest request = new RegisterRequest(email, password, "Test User", role);
        ResponseEntity<UserResponse> response = restTemplate.postForEntity("/api/auth/register", request, UserResponse.class);
        return response.getBody();
    }

    protected String authenticateUser(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/auth/login", request, AuthResponse.class);
        return response.getBody() != null ? response.getBody().token() : null;
    }

    protected ListingResponse createTestListing(String token, CreateListingRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<CreateListingRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ListingResponse> response = restTemplate.postForEntity("/api/listings", entity, ListingResponse.class);
        
        if (response.getStatusCode().isError() || response.getBody() == null) {
            throw new RuntimeException("Failed to create listing - Status: " + response.getStatusCode() + ", Body: " + response.getBody());
        }
        
        return response.getBody();
    }

    protected ListingResponse createApprovedListing(String landlordToken, String adminToken) {
        CreateListingRequest listingRequest = new CreateListingRequest(
                "Test Listing",
                "Test description",
                new BigDecimal("1000.00"),
                "Test City",
                2,
                true,
                false
        );
        ListingResponse listing = createTestListing(landlordToken, listingRequest);

        if (listing == null) {
            throw new RuntimeException("Failed to create listing - createTestListing returned null");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<ListingResponse> approveResponse = restTemplate.exchange(
                "/api/admin/listings/" + listing.id() + "/approve",
                HttpMethod.PUT,
                entity,
                ListingResponse.class
        );

        if (approveResponse.getBody() == null) {
            throw new RuntimeException("Failed to approve listing - approval returned null");
        }

        // Return the approved listing directly from the approve response
        return approveResponse.getBody();
    }
}
