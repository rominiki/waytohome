package com.rominiki.waytohome.integration.concurrency;

import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.dto.UserResponse;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import com.rominiki.waytohome.repository.FavoriteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Concurrency Tests")
class ConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Test
    @DisplayName("Two simultaneous favorite requests for same listing - only one succeeds")
    void addFavorite_concurrentDuplicateRequests_onlyOneSucceeds() throws InterruptedException {
        // Arrange - Create student user
        String studentEmail = "student-concurrent-" + System.nanoTime() + "@test.com";
        UserResponse student = createTestUser(studentEmail, "password123", Role.STUDENT);
        String studentToken = authenticateUser(studentEmail, "password123");

        // Arrange - Create landlord and admin for approved listing
        String landlordEmail = "landlord-concurrent-" + System.nanoTime() + "@test.com";
        createTestUser(landlordEmail, "password123", Role.LANDLORD);
        String landlordToken = authenticateUser(landlordEmail, "password123");

        String adminEmail = "admin-concurrent-" + System.nanoTime() + "@test.com";
        createTestUser(adminEmail, "password123", Role.ADMIN);
        String adminToken = authenticateUser(adminEmail, "password123");

        // Arrange - Create and approve a listing
        ListingResponse listing = createApprovedListing(landlordToken, adminToken);
        Long listingId = listing.id();

        // Arrange - CountDownLatch to synchronize threads
        CountDownLatch startLatch = new CountDownLatch(1);  // Signal to start both threads
        CountDownLatch doneLatch = new CountDownLatch(2);   // Wait for both threads to complete

        // Shared variables to capture results
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<HttpStatusCode> thread1Status = new AtomicReference<>();
        AtomicReference<HttpStatusCode> thread2Status = new AtomicReference<>();

        // Prepare HTTP request
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(studentToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Thread 1: Attempt to add favorite
        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();  // Wait for signal to start
                ResponseEntity<String> response = restTemplate.exchange(
                        "/api/favorites/" + listingId,
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                thread1Status.set(response.getStatusCode());
                if (response.getStatusCode().is2xxSuccessful()) {
                    successCount.incrementAndGet();
                } else if (response.getStatusCode().value() == 409) {
                    conflictCount.incrementAndGet();
                }
            } catch (Exception e) {
                // Handle exceptions
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Attempt to add favorite
        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();  // Wait for signal to start
                ResponseEntity<String> response = restTemplate.exchange(
                        "/api/favorites/" + listingId,
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                thread2Status.set(response.getStatusCode());
                if (response.getStatusCode().is2xxSuccessful()) {
                    successCount.incrementAndGet();
                } else if (response.getStatusCode().value() == 409) {
                    conflictCount.incrementAndGet();
                }
            } catch (Exception e) {
                // Handle exceptions
            } finally {
                doneLatch.countDown();
            }
        });

        // Act - Start both threads
        thread1.start();
        thread2.start();

        // Release both threads at the same time
        startLatch.countDown();

        // Wait for both threads to complete
        doneLatch.await();

        // Assert - Verify only one succeeded, other got 409
        assertThat(successCount.get())
                .as("Exactly one request should succeed")
                .isEqualTo(1);
        assertThat(conflictCount.get())
                .as("Exactly one request should get 409 Conflict")
                .isEqualTo(1);

        // Assert - One thread got success status, one got conflict
        boolean oneSuccessOneConflict = 
                (thread1Status.get().is2xxSuccessful() && thread2Status.get().value() == 409) ||
                (thread2Status.get().is2xxSuccessful() && thread1Status.get().value() == 409);
        assertThat(oneSuccessOneConflict)
                .as("One thread should get 2xx success, the other should get 409 conflict")
                .isTrue();

        // Assert - Verify database has exactly one favorite record
        long favoriteCount = favoriteRepository.count();
        assertThat(favoriteCount)
                .as("Database should contain exactly one favorite record")
                .isGreaterThanOrEqualTo(1);


        ResponseEntity<String> getFavoritesResponse = restTemplate.exchange(
                "/api/favorites",
                HttpMethod.GET,
                entity,
                String.class
        );
        
        // Verify the listing appears exactly once in favorites
        String favoritesBody = getFavoritesResponse.getBody();
        assertThat(favoritesBody)
                .as("Listing should appear exactly once in user's favorites")
                .isNotNull();
        
        // Count occurrences of the listing ID in the response
        int occurrences = 0;
        int index = 0;
        String searchStr = "\"id\":" + listingId;
        while ((index = favoritesBody.indexOf(searchStr, index)) != -1) {
            occurrences++;
            index += searchStr.length();
        }
        
        assertThat(occurrences)
                .as("Listing should appear exactly once in favorites")
                .isEqualTo(1);
    }
}
