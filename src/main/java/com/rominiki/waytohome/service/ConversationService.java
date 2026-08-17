package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.ConversationResponse;
import com.rominiki.waytohome.entity.Conversation;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.exception.DuplicateConversationException;
import com.rominiki.waytohome.exception.ResourceNotFoundException;
import com.rominiki.waytohome.repository.ConversationRepository;
import com.rominiki.waytohome.repository.ListingRepository;
import com.rominiki.waytohome.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConversationResponse startConversation(Long listingId, String userEmail) {
        User student = getUserByEmail(userEmail);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        User landlord = listing.getOwner();

        if (landlord.getId().equals(student.getId())) {
            throw new AccessDeniedException("You cannot start a conversation with yourself");
        }

        // Check if conversation already exists
        if (conversationRepository.findByListingAndStudentAndLandlord(listing, student, landlord).isPresent()) {
            throw new DuplicateConversationException(listingId);
        }

        Conversation conversation = conversationRepository.save(
                Conversation.builder()
                        .listing(listing)
                        .student(student)
                        .landlord(landlord)
                        .build()
        );

        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(String userEmail) {
        User user = getUserByEmail(userEmail);

        return conversationRepository
                .findByStudentOrLandlordOrderByUpdatedAtDesc(user, user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Conversation getConversationEntityForUser(Long conversationId, String userEmail) {
        User user = getUserByEmail(userEmail);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.hasParticipant(user)) {
            throw new AccessDeniedException("You are not part of this conversation");
        }

        return conversation;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),

                conversation.getListing().getId(),
                conversation.getListing().getTitle(),

                conversation.getStudent().getId(),
                conversation.getStudent().getFullName(),

                conversation.getLandlord().getId(),
                conversation.getLandlord().getFullName(),

                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}