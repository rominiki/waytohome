package com.rominiki.waytohome.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rominiki.waytohome.config.SecurityConfig;
import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.enums.ListingStatus;
import com.rominiki.waytohome.security.JwtService;
import com.rominiki.waytohome.security.UserDetailsServiceImpl;
import com.rominiki.waytohome.service.ListingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListingController.class)
@Import(SecurityConfig.class)
class ListingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean
    ListingService listingService;

    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(roles = "STUDENT")
    void create_asStudent_returns403() throws Exception {
        var req = new CreateListingRequest(
                "Cozy studio", "Near campus", BigDecimal.valueOf(750),
                "Fulda", 1, true, false);

        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());   // 403
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void create_asLandlord_returns201() throws Exception {
        var req = new CreateListingRequest(
                "Cozy studio", "Near campus", BigDecimal.valueOf(750),
                "Fulda", 1, true, false);

        var response = new ListingResponse(
                1L, "Cozy studio", "Near campus", BigDecimal.valueOf(750),
                "Fulda", 1, true, ListingStatus.PENDING,
                "Test Landlord", LocalDateTime.now());

        when(listingService.create(any(CreateListingRequest.class), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}