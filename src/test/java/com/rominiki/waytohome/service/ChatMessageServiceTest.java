package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.ChatMessageRequest;
import com.rominiki.waytohome.dto.ChatMessageResponse;
import com.rominiki.waytohome.entity.ChatMessage;
import com.rominiki.waytohome.entity.Conversation;
import com.rominiki.waytohome.entity.Listing;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.repository.ChatMessageRepository;
import com.rominiki.waytohome.repository.ConversationRepository;
import com.rominiki.waytohome.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    ChatMessageRepository chatMessageRepository;

    @Mock
    ConversationRepository conversationRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    ChatMessageService chatMessageService;

    @Test
    void sendMessage_savesMessageAndSendsToRecipient() {
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

        ChatMessage savedMessage = ChatMessage.builder()
                .id(500L)
                .conversation(conversation)
                .sender(student)
                .recipient(landlord)
                .content("Hi, is this still available?")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessageRequest request = new ChatMessageRequest(
                100L,
                "Hi, is this still available?"
        );

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        when(conversationRepository.findById(100L))
                .thenReturn(Optional.of(conversation));

        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(savedMessage);

        ChatMessageResponse response =
                chatMessageService.sendMessage(request, "student@test.com");

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.conversationId()).isEqualTo(100L);
        assertThat(response.senderId()).isEqualTo(1L);
        assertThat(response.recipientId()).isEqualTo(2L);
        assertThat(response.content()).isEqualTo("Hi, is this still available?");
        assertThat(response.read()).isFalse();

        verify(chatMessageRepository).save(any(ChatMessage.class));

        verify(conversationRepository).save(conversation);

        verify(messagingTemplate).convertAndSendToUser(
                eq("landlord@test.com"),
                eq("/queue/messages"),
                any(ChatMessageResponse.class)
        );
    }

    @Test
    void sendMessage_whenUserIsNotParticipant_throwsAccessDenied() {
        User stranger = User.builder()
                .id(99L)
                .email("stranger@test.com")
                .fullName("Stranger User")
                .role(Role.STUDENT)
                .build();

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

        ChatMessageRequest request = new ChatMessageRequest(
                100L,
                "Trying to send a message"
        );

        when(userRepository.findByEmail("stranger@test.com"))
                .thenReturn(Optional.of(stranger));

        when(conversationRepository.findById(100L))
                .thenReturn(Optional.of(conversation));

        assertThatThrownBy(() ->
                chatMessageService.sendMessage(request, "stranger@test.com")
        ).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not part");

        verify(chatMessageRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(),
                anyString(),
                any()
        );
    }

    @Test
    void getMessages_whenUserIsParticipant_returnsMessagesInOrder() {
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

        ChatMessage message1 = ChatMessage.builder()
                .id(501L)
                .conversation(conversation)
                .sender(student)
                .recipient(landlord)
                .content("Hello")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage message2 = ChatMessage.builder()
                .id(502L)
                .conversation(conversation)
                .sender(landlord)
                .recipient(student)
                .content("Hi")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        when(conversationRepository.findById(100L))
                .thenReturn(Optional.of(conversation));

        when(chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                .thenReturn(List.of(message1, message2));

        List<ChatMessageResponse> responses =
                chatMessageService.getMessages(100L, "student@test.com");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).content()).isEqualTo("Hello");
        assertThat(responses.get(1).content()).isEqualTo("Hi");
    }

    @Test
    void markMessagesAsRead_marksOnlyMessagesReceivedByCurrentUser() {
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

        ChatMessage receivedByStudent = ChatMessage.builder()
                .id(501L)
                .conversation(conversation)
                .sender(landlord)
                .recipient(student)
                .content("Hi student")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage sentByStudent = ChatMessage.builder()
                .id(502L)
                .conversation(conversation)
                .sender(student)
                .recipient(landlord)
                .content("Hi landlord")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        when(conversationRepository.findById(100L))
                .thenReturn(Optional.of(conversation));

        when(chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                .thenReturn(List.of(receivedByStudent, sentByStudent));

        chatMessageService.markMessagesAsRead(100L, "student@test.com");

        assertThat(receivedByStudent.isRead()).isTrue();
        assertThat(sentByStudent.isRead()).isFalse();

        verify(chatMessageRepository).saveAll(List.of(receivedByStudent, sentByStudent));
    }
}