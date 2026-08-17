package com.rominiki.waytohome.integration.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rominiki.waytohome.dto.ChatMessageRequest;
import com.rominiki.waytohome.dto.ChatMessageResponse;
import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.dto.LoginRequest;
import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.dto.StartConversationRequest;
import com.rominiki.waytohome.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WebSocketAuthenticatedMessagingTest {

    @LocalServerPort
    int port;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void authenticatedStudentCanSendMessageToLandlordOverWebSocket() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        String studentEmail = "student-" + unique + "@test.com";
        String landlordEmail = "landlord-" + unique + "@test.com";
        String adminEmail = "admin-" + unique + "@test.com";

        register(studentEmail, "password123", "Student User", Role.STUDENT);
        register(landlordEmail, "password123", "Landlord User", Role.LANDLORD);
        register(adminEmail, "password123", "Admin User", Role.ADMIN);

        String studentToken = login(studentEmail, "password123");
        String landlordToken = login(landlordEmail, "password123");
        String adminToken = login(adminEmail, "password123");

        Long listingId = createListingAsLandlord(landlordToken);
        approveListingAsAdmin(listingId, adminToken);
        Long conversationId = startConversationAsStudent(listingId, studentToken);

        WebSocketStompClient stompClient = createStompClient();

        String url = "ws://localhost:" + port + "/ws/websocket";

        StompSession studentSession = connectWithToken(
                stompClient,
                url,
                studentToken
        );

        StompSession landlordSession = connectWithToken(
                stompClient,
                url,
                landlordToken
        );

        BlockingQueue<ChatMessageResponse> sentQueue = new LinkedBlockingQueue<>();
        BlockingQueue<ChatMessageResponse> receivedQueue = new LinkedBlockingQueue<>();

        studentSession.subscribe(
                "/user/queue/sent",
                new ChatMessageFrameHandler(sentQueue)
        );

        landlordSession.subscribe(
                "/user/queue/messages",
                new ChatMessageFrameHandler(receivedQueue)
        );

        ChatMessageRequest request = new ChatMessageRequest(
                conversationId,
                "Hi, is this apartment still available?"
        );

        studentSession.send("/app/chat.send", request);

        ChatMessageResponse sentMessage = sentQueue.poll(5, TimeUnit.SECONDS);
        ChatMessageResponse receivedMessage = receivedQueue.poll(5, TimeUnit.SECONDS);

        assertThat(sentMessage).isNotNull();
        assertThat(sentMessage.conversationId()).isEqualTo(conversationId);
        assertThat(sentMessage.content()).isEqualTo("Hi, is this apartment still available?");
        assertThat(sentMessage.senderName()).isEqualTo("Student User");
        assertThat(sentMessage.recipientName()).isEqualTo("Landlord User");
        assertThat(sentMessage.read()).isFalse();

        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.conversationId()).isEqualTo(conversationId);
        assertThat(receivedMessage.content()).isEqualTo("Hi, is this apartment still available?");
        assertThat(receivedMessage.senderName()).isEqualTo("Student User");
        assertThat(receivedMessage.recipientName()).isEqualTo("Landlord User");
        assertThat(receivedMessage.read()).isFalse();

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Hi, is this apartment still available?"))
                .andExpect(jsonPath("$.content[0].senderName").value("Student User"))
                .andExpect(jsonPath("$.content[0].recipientName").value("Landlord User"));

        studentSession.disconnect();
        landlordSession.disconnect();
    }

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient stompClient =
                new WebSocketStompClient(new StandardWebSocketClient());

        MappingJackson2MessageConverter converter =
                new MappingJackson2MessageConverter();

        converter.setObjectMapper(objectMapper);

        stompClient.setMessageConverter(converter);

        return stompClient;
    }

    private StompSession connectWithToken(
            WebSocketStompClient stompClient,
            String url,
            String token
    ) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        return stompClient.connectAsync(
                url,
                new WebSocketHttpHeaders(),
                connectHeaders,
                new TestStompSessionHandler()
        ).get(5, TimeUnit.SECONDS);
    }

    private static class ChatMessageFrameHandler implements StompFrameHandler {

        private final BlockingQueue<ChatMessageResponse> queue;

        private ChatMessageFrameHandler(BlockingQueue<ChatMessageResponse> queue) {
            this.queue = queue;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return ChatMessageResponse.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            queue.offer((ChatMessageResponse) payload);
        }
    }

    private void register(String email, String password, String fullName, Role role)
            throws Exception {
        RegisterRequest request = new RegisterRequest(
                email,
                password,
                fullName,
                role
        );

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

        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

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

        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return json.get("id").asLong();
    }

    private void approveListingAsAdmin(Long listingId, String adminToken)
            throws Exception {
        mockMvc.perform(put("/api/admin/listings/" + listingId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private Long startConversationAsStudent(Long listingId, String studentToken)
            throws Exception {
        StartConversationRequest request = new StartConversationRequest(listingId);

        var result = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listingId))
                .andReturn();

        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return json.get("id").asLong();
    }
}