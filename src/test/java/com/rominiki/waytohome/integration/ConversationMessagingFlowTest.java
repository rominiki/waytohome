package com.rominiki.waytohome.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rominiki.waytohome.dto.ChatMessageRequest;
import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.dto.LoginRequest;
import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.dto.StartConversationRequest;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.service.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConversationMessagingFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ChatMessageService chatMessageService;

    @Test
    void studentCanStartConversationSendMessageAndLoadHistory() throws Exception {
        register("student@test.com", "password123", "Student User", Role.STUDENT);
        register("landlord@test.com", "password123", "Landlord User", Role.LANDLORD);
        register("admin@test.com", "password123", "Admin User", Role.ADMIN);

        String studentToken = login("student@test.com", "password123");
        String landlordToken = login("landlord@test.com", "password123");
        String adminToken = login("admin@test.com", "password123");

        Long listingId = createListingAsLandlord(landlordToken);

        approveListingAsAdmin(listingId, adminToken);

        Long conversationId = startConversationAsStudent(listingId, studentToken);

        ChatMessageRequest messageRequest = new ChatMessageRequest(
                conversationId,
                "Hi, is this apartment still available?"
        );

        chatMessageService.sendMessage(messageRequest, "student@test.com");

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hi, is this apartment still available?"))
                .andExpect(jsonPath("$[0].senderName").value("Student User"))
                .andExpect(jsonPath("$[0].recipientName").value("Landlord User"))
                .andExpect(jsonPath("$[0].read").value(false));

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conversationId))
                .andExpect(jsonPath("$[0].listingTitle").value("Cozy studio"));

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + landlordToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conversationId))
                .andExpect(jsonPath("$[0].listingTitle").value("Cozy studio"));
    }

    @Test
    void unauthenticatedUserCannotAccessConversationEndpoints() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/conversations/1/messages"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/conversations/1/read"))
                .andExpect(status().isForbidden());
    }

    private void register(String email, String password, String fullName, Role role)
            throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, fullName, role);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private Long createListingAsLandlord(String landlordToken) throws Exception {
        CreateListingRequest request = new CreateListingRequest(
                "Cozy studio",
                "Near campus",
                BigDecimal.valueOf(750),
                "Fulda",
                1,
                true,
                false
        );

        var result = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private void approveListingAsAdmin(Long listingId, String adminToken) throws Exception {
        mockMvc.perform(put("/api/admin/listings/" + listingId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private Long startConversationAsStudent(Long listingId, String studentToken) throws Exception {
        StartConversationRequest request = new StartConversationRequest(listingId);

        var result = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listingId))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}