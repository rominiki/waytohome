package com.rominiki.waytohome.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rominiki.waytohome.config.SecurityConfig;
import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.entity.User;
import com.rominiki.waytohome.enums.Role;
import com.rominiki.waytohome.security.JwtAuthenticationFilter;
import com.rominiki.waytohome.security.JwtService;
import com.rominiki.waytohome.security.UserDetailsServiceImpl;
import com.rominiki.waytohome.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    AuthenticationManager authenticationManager;

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
    void register_validRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "user@test.com",
                "password123",
                "Test User",
                Role.STUDENT
        );

        User fakeUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .password("$2a$HASH")
                .fullName("Test User")
                .role(Role.STUDENT)
                .build();

        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(fakeUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "notanemail",
                "password123",
                "Test User",
                Role.STUDENT
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}