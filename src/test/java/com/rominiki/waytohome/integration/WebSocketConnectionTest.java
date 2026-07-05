package com.rominiki.waytohome.integration;

import com.rominiki.waytohome.security.JwtChannelInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketConnectionTest {

    @LocalServerPort
    int port;

    @MockitoBean
    JwtChannelInterceptor jwtChannelInterceptor;

    @BeforeEach
    void setUpJwtChannelInterceptor() {
        when(jwtChannelInterceptor.preSend(
                any(Message.class),
                any(MessageChannel.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void websocketEndpoint_acceptsConnection() throws Exception {
        WebSocketStompClient stompClient =
                new WebSocketStompClient(new StandardWebSocketClient());

        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String url = "ws://localhost:" + port + "/ws/websocket";

        var sessionFuture = stompClient.connectAsync(
                url,
                new TestStompSessionHandler()
        );

        var session = sessionFuture.get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();

        session.disconnect();
    }
}