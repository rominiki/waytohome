package com.rominiki.waytohome.controller;

import com.rominiki.waytohome.dto.ChatMessageResponse;
import com.rominiki.waytohome.dto.ConversationResponse;
import com.rominiki.waytohome.dto.StartConversationRequest;
import com.rominiki.waytohome.service.ChatMessageService;
import com.rominiki.waytohome.service.ConversationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.rominiki.waytohome.dto.ChatMessageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatMessageService chatMessageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse startConversation(
            @Valid @RequestBody StartConversationRequest request,
            Authentication auth
    ) {
        return conversationService.startConversation(
                request.listingId(),
                auth.getName()
        );
    }

    @GetMapping
    public List<ConversationResponse> getMyConversations(Authentication auth) {
        return conversationService.getMyConversations(auth.getName());
    }

    @GetMapping("/{conversationId}/messages")
    public Page<ChatMessageResponse> getMessages(
            @PathVariable Long conversationId,
            Pageable pageable,
            Authentication auth
    ) {
        return chatMessageService.getMessagesPaginated(
                conversationId,
                auth.getName(),
                pageable
        );
    }

    @PutMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markMessagesAsRead(
            @PathVariable Long conversationId,
            Authentication auth
    ) {
        chatMessageService.markMessagesAsRead(conversationId, auth.getName());
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessageByRest(
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatMessageRequest request,
            Authentication auth
    ) {
        ChatMessageRequest fixedRequest = new ChatMessageRequest(
                conversationId,
                request.content()
        );

        return chatMessageService.sendMessage(fixedRequest, auth.getName());
    }
}