package com.rominiki.waytohome.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rominiki.waytohome.config.SecurityConfig;
import com.rominiki.waytohome.dto.ChatMessageRequest;
import com.rominiki.waytohome.dto.ChatMessageResponse;
import com.rominiki.waytohome.dto.ConversationResponse;
import com.rominiki.waytohome.dto.StartConversationRequest;
import com.rominiki.waytohome.security.JwtAuthenticationFilter;
import com.rominiki.waytohome.security.UserDetailsServiceImpl;
import com.rominiki.waytohome.service.ChatMessageService;
import com.rominiki.waytohome.service.ConversationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationController.class)
@Import(SecurityConfig.class)
class ConversationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ConversationService conversationService;

    @MockitoBean
    ChatMessageService chatMessageService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthFilter;

    @MockitoBean
    UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUpJwtFilterMock() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(
                any(ServletRequest.class),
                any(ServletResponse.class),
                any(FilterChain.class)
        );
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void startConversation_authenticatedUser_returnsCreatedConversation() throws Exception {
        StartConversationRequest request = new StartConversationRequest(10L);

        ConversationResponse response = new ConversationResponse(
                100L,
                10L,
                "Cozy Studio",
                1L,
                "Student User",
                2L,
                "Landlord User",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(conversationService.startConversation(10L, "student@test.com"))
                .thenReturn(response);

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.listingId").value(10L))
                .andExpect(jsonPath("$.listingTitle").value("Cozy Studio"))
                .andExpect(jsonPath("$.studentId").value(1L))
                .andExpect(jsonPath("$.landlordId").value(2L));

        verify(conversationService).startConversation(10L, "student@test.com");
    }

    @Test
    void startConversation_withoutLogin_returns403() throws Exception {
        StartConversationRequest request = new StartConversationRequest(10L);

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conversationService);
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void getMyConversations_authenticatedUser_returnsList() throws Exception {
        ConversationResponse response = new ConversationResponse(
                100L,
                10L,
                "Cozy Studio",
                1L,
                "Student User",
                2L,
                "Landlord User",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(conversationService.getMyConversations("student@test.com"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].listingTitle").value("Cozy Studio"));

        verify(conversationService).getMyConversations("student@test.com");
    }

    @Test
    void getMyConversations_withoutLogin_returns403() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conversationService);
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void getMessages_authenticatedUser_returnsPaginatedMessages() throws Exception {
        ChatMessageResponse message = new ChatMessageResponse(
                500L,
                100L,
                1L,
                "Student User",
                2L,
                "Landlord User",
                "Hi, is this available?",
                LocalDateTime.now(),
                false
        );

        Page<ChatMessageResponse> page = new PageImpl<>(List.of(message));

        when(chatMessageService.getMessagesPaginated(
                eq(100L),
                eq("student@test.com"),
                any(Pageable.class)
        )).thenReturn(page);

        mockMvc.perform(get("/api/conversations/100/messages")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(500L))
                .andExpect(jsonPath("$.content[0].conversationId").value(100L))
                .andExpect(jsonPath("$.content[0].content").value("Hi, is this available?"))
                .andExpect(jsonPath("$.content[0].read").value(false));

        verify(chatMessageService).getMessagesPaginated(
                eq(100L),
                eq("student@test.com"),
                any(Pageable.class)
        );
    }

    @Test
    void getMessages_withoutLogin_returns403() throws Exception {
        mockMvc.perform(get("/api/conversations/100/messages"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(chatMessageService);
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void sendMessageByRest_authenticatedUser_returnsCreatedMessage() throws Exception {
        ChatMessageRequest request = new ChatMessageRequest(
                100L,
                "Hi, is this available?"
        );

        ChatMessageResponse response = new ChatMessageResponse(
                500L,
                100L,
                1L,
                "Student User",
                2L,
                "Landlord User",
                "Hi, is this available?",
                LocalDateTime.now(),
                false
        );

        when(chatMessageService.sendMessage(
                any(ChatMessageRequest.class),
                eq("student@test.com")
        )).thenReturn(response);

        mockMvc.perform(post("/api/conversations/100/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500L))
                .andExpect(jsonPath("$.conversationId").value(100L))
                .andExpect(jsonPath("$.content").value("Hi, is this available?"))
                .andExpect(jsonPath("$.senderName").value("Student User"))
                .andExpect(jsonPath("$.recipientName").value("Landlord User"))
                .andExpect(jsonPath("$.read").value(false));

        verify(chatMessageService).sendMessage(
                any(ChatMessageRequest.class),
                eq("student@test.com")
        );
    }

    @Test
    void sendMessageByRest_withoutLogin_returns403() throws Exception {
        ChatMessageRequest request = new ChatMessageRequest(
                100L,
                "Hi, is this available?"
        );

        mockMvc.perform(post("/api/conversations/100/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(chatMessageService);
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void markMessagesAsRead_authenticatedUser_returns204() throws Exception {
        mockMvc.perform(put("/api/conversations/100/read"))
                .andExpect(status().isNoContent());

        verify(chatMessageService).markMessagesAsRead(100L, "student@test.com");
    }

    @Test
    void markMessagesAsRead_withoutLogin_returns403() throws Exception {
        mockMvc.perform(put("/api/conversations/100/read"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(chatMessageService);
    }
}