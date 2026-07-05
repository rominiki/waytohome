package com.rominiki.waytohome.controller;

import com.rominiki.waytohome.dto.ChatMessageRequest;
import com.rominiki.waytohome.dto.ChatMessageResponse;
import com.rominiki.waytohome.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    @SendToUser("/queue/sent")
    public ChatMessageResponse sendMessage(
            @Valid ChatMessageRequest request,
            Authentication auth
    ) {
        return chatMessageService.sendMessage(request, auth.getName());
    }
}