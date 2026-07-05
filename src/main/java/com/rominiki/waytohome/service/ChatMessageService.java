package com.rominiki.waytohome.service;

import com.rominiki.waytohome.dto.ChatMessageResponse;
import com.rominiki.waytohome.dto.ChatMessageRequest;
import com.rominiki.waytohome.entity.ChatMessage;
import com.rominiki.waytohome.entity.Conversation;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.exception.ResourceNotFoundException;
import com.rominiki.waytohome.repository.ChatMessageRepository;
import com.rominiki.waytohome.repository.ConversationRepository;
import com.rominiki.waytohome.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, String senderEmail) {
        User sender = getUserByEmail(senderEmail);

        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.hasParticipant(sender)) {
            throw new AccessDeniedException("You are not part of this conversation");
        }

        User recipient = conversation.otherParticipant(sender);

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .recipient(recipient)
                .content(request.content())
                .read(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        conversation.touch();
        conversationRepository.save(conversation);

        ChatMessageResponse response = toResponse(savedMessage);

        messagingTemplate.convertAndSendToUser(
                recipient.getEmail(),
                "/queue/messages",
                response
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long conversationId, String userEmail) {
        User user = getUserByEmail(userEmail);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.hasParticipant(user)) {
            throw new AccessDeniedException("You are not part of this conversation");
        }

        return chatMessageRepository
                .findByConversationOrderByCreatedAtAsc(conversation)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markMessagesAsRead(Long conversationId, String userEmail) {
        User user = getUserByEmail(userEmail);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.hasParticipant(user)) {
            throw new AccessDeniedException("You are not part of this conversation");
        }

        List<ChatMessage> messages = chatMessageRepository
                .findByConversationOrderByCreatedAtAsc(conversation);

        messages.stream()
                .filter(message -> message.getRecipient().getId().equals(user.getId()))
                .filter(message -> !message.isRead())
                .forEach(message -> message.setRead(true));

        chatMessageRepository.saveAll(messages);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),

                message.getConversation().getId(),

                message.getSender().getId(),
                message.getSender().getFullName(),

                message.getRecipient().getId(),
                message.getRecipient().getFullName(),

                message.getContent(),
                message.getCreatedAt(),
                message.isRead()
        );
    }
}