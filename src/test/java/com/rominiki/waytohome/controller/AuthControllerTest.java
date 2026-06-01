package com.rominiki.waytohome.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rominiki.waytohome.config.SecurityConfig;
import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean UserService userService;

    @Test
    void register_validRequest_returns201() throws Exception {
        var request = new RegisterRequest(
                "user@test.com", "password123", "Test User", Role.STUDENT);
        var fakeUser = User.builder()
                .id(1L).email("user@test.com")
                .password("$2a$HASH").fullName("Test User")
                .role(Role.STUDENT).build();
        when(userService.register(any())).thenReturn(fakeUser);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // 201
                .andExpect(jsonPath("$.email").value("user@test.com"));
    }
    @Test
    void register_invalidEmail_returns400() throws Exception {
        var request = new RegisterRequest(
                "notanemail", "password123", "Test User", Role.STUDENT);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
