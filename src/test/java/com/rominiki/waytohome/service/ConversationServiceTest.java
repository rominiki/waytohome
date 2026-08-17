package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.ConversationResponse;
import com.rominiki.waytohome.entity.Conversation;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.repository.ConversationRepository;
import com.rominiki.waytohome.repository.ListingRepository;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    ConversationRepository conversationRepository;

    @Mock
    ListingRepository listingRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ConversationService conversationService;

    @Test
    void startConversation_whenNotExisting_createsIt() {
        User student = User.builder()
                .id(1L)
                .email("student@test.com")
                .fullName("Student User")
                .role(Role.STUDENT)
                .build();

        User landlord = User.builder()
                .id(2L)
                .email("landlord@test.com")
                .fullName("Landlord User")
                .role(Role.LANDLORD)
                .build();

        Listing listing = Listing.builder()
                .id(10L)
                .title("Cozy Studio")
                .owner(landlord)
                .build();

        Conversation savedConversation = Conversation.builder()
                .id(100L)
                .listing(listing)
                .student(student)
                .landlord(landlord)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        when(listingRepository.findById(10L))
                .thenReturn(Optional.of(listing));

        when(conversationRepository.findByListingAndStudentAndLandlord(
                listing, student, landlord
        )).thenReturn(Optional.empty());

        when(conversationRepository.save(any(Conversation.class)))
                .thenReturn(savedConversation);

        ConversationResponse response =
                conversationService.startConversation(10L, "student@test.com");

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.listingId()).isEqualTo(10L);
        assertThat(response.listingTitle()).isEqualTo("Cozy Studio");
        assertThat(response.studentId()).isEqualTo(1L);
        assertThat(response.landlordId()).isEqualTo(2L);

        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void startConversation_whenAlreadyExisting_throwsDuplicateConversationException() {
        User student = User.builder()
                .id(1L)
                .email("student@test.com")
                .fullName("Student User")
                .role(Role.STUDENT)
                .build();

        User landlord = User.builder()
                .id(2L)
                .email("landlord@test.com")
                .fullName("Landlord User")
                .role(Role.LANDLORD)
                .build();

        Listing listing = Listing.builder()
                .id(10L)
                .title("Cozy Studio")
                .owner(landlord)
                .build();

        Conversation existingConversation = Conversation.builder()
                .id(100L)
                .listing(listing)
                .student(student)
                .landlord(landlord)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        when(listingRepository.findById(10L))
                .thenReturn(Optional.of(listing));

        when(conversationRepository.findByListingAndStudentAndLandlord(
                listing, student, landlord
        )).thenReturn(Optional.of(existingConversation));

        assertThatThrownBy(() ->
                conversationService.startConversation(10L, "student@test.com")
        ).isInstanceOf(com.rominiki.waytohome.exception.DuplicateConversationException.class);

        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void startConversation_whenUserIsListingOwner_throwsAccessDenied() {
        User landlord = User.builder()
                .id(2L)
                .email("landlord@test.com")
                .fullName("Landlord User")
                .role(Role.LANDLORD)
                .build();

        Listing listing = Listing.builder()
                .id(10L)
                .title("Cozy Studio")
                .owner(landlord)
                .build();

        when(userRepository.findByEmail("landlord@test.com"))
                .thenReturn(Optional.of(landlord));

        when(listingRepository.findById(10L))
                .thenReturn(Optional.of(listing));

        assertThatThrownBy(() ->
                conversationService.startConversation(10L, "landlord@test.com")
        ).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("yourself");

        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getMyConversations_returnsUserConversationsNewestFirst() {
        User student = User.builder()
                .id(1L)
                .email("student@test.com")
                .fullName("Student User")
                .role(Role.STUDENT)
                .build();

        User landlord = User.builder()
                .id(2L)
                .email("landlord@test.com")
                .fullName("Landlord User")
                .role(Role.LANDLORD)
                .build();

        Listing listing = Listing.builder()
                .id(10L)
                .title("Cozy Studio")
                .owner(landlord)
                .build();

        Conversation conversation = Conversation.builder()
                .id(100L)
                .listing(listing)
                .student(student)
                .landlord(landlord)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        when(conversationRepository.findByStudentOrLandlordOrderByUpdatedAtDesc(
                student, student
        )).thenReturn(List.of(conversation));

        List<ConversationResponse> responses =
                conversationService.getMyConversations("student@test.com");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(100L);
        assertThat(responses.get(0).listingTitle()).isEqualTo("Cozy Studio");
    }
}