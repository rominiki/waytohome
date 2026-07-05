package com.rominiki.waytohome.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WebSocketConfigTest {

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    @Test
    void websocketMessagingTemplate_isCreated() {
        assertThat(simpMessagingTemplate).isNotNull();
    }
}