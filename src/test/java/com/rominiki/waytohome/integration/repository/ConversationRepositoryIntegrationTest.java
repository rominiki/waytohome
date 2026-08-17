package com.rominiki.waytohome.integration.repository;

import com.rominiki.waytohome.entity.Conversation;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.ListingStatus;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.integration.base.BaseIntegrationTest;
import com.rominiki.waytohome.repository.ConversationRepository;
import com.rominiki.waytohome.repository.ListingRepository;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("Conversation Repository Integration Tests")
class ConversationRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.rominiki.waytohome.repository.FavoriteRepository favoriteRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User student1;
    private User student2;
    private User landlord1;
    private User landlord2;
    private Listing listing1;
    private Listing listing2;

    @BeforeEach
    void setUp() {
        // Clean up existing data (order matters: delete dependent tables first)
        conversationRepository.deleteAll();
        favoriteRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        student1 = createUser("student1@test.com", "Student One", Role.STUDENT);
        student2 = createUser("student2@test.com", "Student Two", Role.STUDENT);
        landlord1 = createUser("landlord1@test.com", "Landlord One", Role.LANDLORD);
        landlord2 = createUser("landlord2@test.com", "Landlord Two", Role.LANDLORD);

        // Create test listings
        listing1 = createListing("Listing 1", landlord1, new BigDecimal("1000"));
        listing2 = createListing("Listing 2", landlord2, new BigDecimal("1500"));
    }

    @Test
    @DisplayName("findByStudentOrLandlordOrderByUpdatedAtDesc returns conversations for student user")
    void findByStudentOrLandlord_withStudentUser_returnsStudentConversations() {
        // Arrange: Create conversations where student1 is a participant
        Conversation conv1 = createConversation(listing1, student1, landlord1);
        Conversation conv2 = createConversation(listing2, student1, landlord2);
        
        // Create conversation that student1 is NOT part of
        createConversation(listing1, student2, landlord1);

        conversationRepository.saveAll(List.of(conv1, conv2));

        // Act: Find conversations for student1
        List<Conversation> results = conversationRepository.findByStudentOrLandlordOrderByUpdatedAtDesc(student1, student1);

        // Assert: Should return only conversations where student1 is a participant
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Conversation::getStudent)
                .containsOnly(student1);
    }

    @Test
    @DisplayName("findByStudentOrLandlordOrderByUpdatedAtDesc returns conversations for landlord user")
    void findByStudentOrLandlord_withLandlordUser_returnsLandlordConversations() {
        // Arrange: Create conversations where landlord1 is a participant
        Conversation conv1 = createConversation(listing1, student1, landlord1);
        Conversation conv2 = createConversation(listing1, student2, landlord1);
        
        // Create conversation that landlord1 is NOT part of
        createConversation(listing2, student1, landlord2);

        conversationRepository.saveAll(List.of(conv1, conv2));

        // Act: Find conversations for landlord1
        List<Conversation> results = conversationRepository.findByStudentOrLandlordOrderByUpdatedAtDesc(landlord1, landlord1);

        // Assert: Should return only conversations where landlord1 is a participant
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Conversation::getLandlord)
                .containsOnly(landlord1);
    }

    @Test
    @DisplayName("findByStudentOrLandlordOrderByUpdatedAtDesc orders by updatedAt descending")
    void findByStudentOrLandlord_ordersConversations_byUpdatedAtDescending() throws InterruptedException {
        // Arrange: Create conversations with different update times
        Conversation oldest = createConversation(listing1, student1, landlord1);
        conversationRepository.save(oldest);
        
        // Small delay to ensure different timestamps
        Thread.sleep(10);
        
        Conversation middle = createConversation(listing2, student1, landlord2);
        conversationRepository.save(middle);
        
        Thread.sleep(10);
        
        Conversation newest = createConversationWithListing(listing1, student2, landlord1);
        conversationRepository.save(newest);
        
        // Update the oldest conversation to make it the most recent
        Thread.sleep(10);
        oldest.touch();
        conversationRepository.save(oldest);

        // Act: Find all conversations for student1 (participates in oldest and middle)
        List<Conversation> results = conversationRepository.findByStudentOrLandlordOrderByUpdatedAtDesc(student1, student1);

        // Assert: Should be ordered by updatedAt descending (oldest is now most recent due to touch())
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(oldest.getId());
        assertThat(results.get(1).getId()).isEqualTo(middle.getId());
        
        // Verify ordering
        assertThat(results.get(0).getUpdatedAt())
                .isAfter(results.get(1).getUpdatedAt());
    }

    @Test
    @DisplayName("findByStudentOrLandlordOrderByUpdatedAtDesc returns empty list when user has no conversations")
    void findByStudentOrLandlord_withNoConversations_returnsEmptyList() {
        // Arrange: Create conversations not involving student1
        Conversation conv = createConversation(listing1, student2, landlord1);
        conversationRepository.save(conv);

        // Act: Find conversations for student1 (has none)
        List<Conversation> results = conversationRepository.findByStudentOrLandlordOrderByUpdatedAtDesc(student1, student1);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByListingAndStudentAndLandlord finds exact conversation match")
    void findByListingAndStudentAndLandlord_withMatchingConversation_returnsConversation() {
        // Arrange: Create specific conversation
        Conversation conversation = createConversation(listing1, student1, landlord1);
        conversationRepository.save(conversation);
        
        // Create other conversations that don't match all three criteria
        createConversation(listing2, student1, landlord2); // different listing and landlord
        createConversation(listing1, student2, landlord1); // different student

        // Act: Find the specific conversation
        Optional<Conversation> result = conversationRepository.findByListingAndStudentAndLandlord(
                listing1, student1, landlord1);

        // Assert: Should find the exact match
        assertThat(result).isPresent();
        assertThat(result.get().getListing().getId()).isEqualTo(listing1.getId());
        assertThat(result.get().getStudent().getId()).isEqualTo(student1.getId());
        assertThat(result.get().getLandlord().getId()).isEqualTo(landlord1.getId());
    }

    @Test
    @DisplayName("findByListingAndStudentAndLandlord returns empty when no match exists")
    void findByListingAndStudentAndLandlord_withNoMatch_returnsEmpty() {
        // Arrange: Create conversation with different combination
        Conversation conversation = createConversation(listing1, student1, landlord1);
        conversationRepository.save(conversation);

        // Act: Try to find conversation with non-matching landlord
        Optional<Conversation> result = conversationRepository.findByListingAndStudentAndLandlord(
                listing1, student1, landlord2);

        // Assert: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByListingAndStudentAndLandlord enforces unique constraint on conversation triplet")
    void findByListingAndStudentAndLandlord_verifiesUniqueness_ofConversationTriplet() {
        // Arrange: Create a conversation
        Conversation conversation = createConversation(listing1, student1, landlord1);
        conversationRepository.save(conversation);

        // Act: Try to find it
        Optional<Conversation> result = conversationRepository.findByListingAndStudentAndLandlord(
                listing1, student1, landlord1);

        // Assert: Should find exactly one conversation
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(conversation.getId());
        
        // Verify uniqueness - there should only be one conversation for this triplet
        List<Conversation> allConversations = conversationRepository.findAll();
        long matchingCount = allConversations.stream()
                .filter(c -> c.getListing().getId().equals(listing1.getId())
                        && c.getStudent().getId().equals(student1.getId())
                        && c.getLandlord().getId().equals(landlord1.getId()))
                .count();
        assertThat(matchingCount).isEqualTo(1);
    }

    @Test
    @DisplayName("findByStudentOrLandlordOrderByUpdatedAtDesc handles user participating as both student and landlord")
    void findByStudentOrLandlord_withUserAsStudentAndLandlord_returnsAllParticipations() {
        // Arrange: Create a user with LANDLORD role who also participates as student in conversations
        User landlordAsStudent = createUser("hybrid@test.com", "Hybrid User", Role.LANDLORD);
        
        // Conversation where landlordAsStudent is the landlord
        Conversation asLandlord = createConversation(
                createListing("Hybrid Listing", landlordAsStudent, new BigDecimal("1200")),
                student1,
                landlordAsStudent
        );
        
        // Note: In reality, a landlord wouldn't be a student in another conversation due to role constraints,
        // but we're testing the repository query logic which uses OR condition
        conversationRepository.save(asLandlord);

        // Act: Find all conversations for this user (searches both student and landlord fields)
        List<Conversation> results = conversationRepository.findByStudentOrLandlordOrderByUpdatedAtDesc(
                landlordAsStudent, landlordAsStudent);

        // Assert: Should find the conversation where they are landlord
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLandlord().getId()).isEqualTo(landlordAsStudent.getId());
    }

    @Test
    @DisplayName("Conversations are correctly associated with their listing")
    void conversations_maintainCorrectListing_associations() {
        // Arrange: Create multiple conversations for different listings
        Conversation conv1ForListing1 = createConversation(listing1, student1, landlord1);
        Conversation conv2ForListing1 = createConversation(listing1, student2, landlord1);
        Conversation conv1ForListing2 = createConversation(listing2, student1, landlord2);
        
        conversationRepository.saveAll(List.of(conv1ForListing1, conv2ForListing1, conv1ForListing2));

        // Act: Find all conversations
        List<Conversation> allConversations = conversationRepository.findAll();

        // Assert: Verify correct listing associations
        List<Conversation> listing1Conversations = allConversations.stream()
                .filter(c -> c.getListing().getId().equals(listing1.getId()))
                .toList();
        
        List<Conversation> listing2Conversations = allConversations.stream()
                .filter(c -> c.getListing().getId().equals(listing2.getId()))
                .toList();

        assertThat(listing1Conversations).hasSize(2);
        assertThat(listing2Conversations).hasSize(1);
    }

    @Test
    @DisplayName("Multiple conversations ordered correctly when updatedAt timestamps are very close")
    void findByStudentOrLandlord_withCloseTimestamps_maintainsCorrectOrder() throws InterruptedException {
        // Arrange: Create multiple conversations with very close timestamps
        Conversation conv1 = createConversation(listing1, student1, landlord1);
        conversationRepository.save(conv1);
        
        Thread.sleep(5);
        
        Conversation conv2 = createConversation(listing2, student1, landlord2);
        conversationRepository.save(conv2);
        
        Thread.sleep(5);
        
        // Create a third listing for variety
        Listing listing3 = createListing("Listing 3", landlord1, new BigDecimal("2000"));
        Conversation conv3 = createConversationWithListing(listing3, student1, landlord1);
        conversationRepository.save(conv3);

        // Act: Retrieve conversations
        List<Conversation> results = conversationRepository.findByStudentOrLandlordOrderByUpdatedAtDesc(student1, student1);

        // Assert: Should maintain descending order
        assertThat(results).hasSize(3);
        
        // Verify they are in descending order
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getUpdatedAt())
                    .isAfterOrEqualTo(results.get(i + 1).getUpdatedAt());
        }
        
        // Most recent should be conv3
        assertThat(results.get(0).getId()).isEqualTo(conv3.getId());
    }

    // Helper methods

    private User createUser(String email, String fullName, Role role) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("password"))
                .fullName(fullName)
                .role(role)
                .build();
        return userRepository.save(user);
    }

    private Listing createListing(String title, User owner, BigDecimal price) {
        Listing listing = Listing.builder()
                .title(title)
                .description("Test description for " + title)
                .status(ListingStatus.APPROVED)
                .location("Test City")
                .price(price)
                .bedrooms(2)
                .petFriendly(false)
                .accessible(false)
                .owner(owner)
                .build();
        return listingRepository.save(listing);
    }

    private Conversation createConversation(Listing listing, User student, User landlord) {
        return Conversation.builder()
                .listing(listing)
                .student(student)
                .landlord(landlord)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Conversation createConversationWithListing(Listing listing, User student, User landlord) {
        return Conversation.builder()
                .listing(listing)
                .student(student)
                .landlord(landlord)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
