package com.rominiki.waytohome.controller;

import com.rominiki.waytohome.config.SecurityConfig;
import com.rominiki.waytohome.dto.ListingResponse;
import com.rominiki.waytohome.enums.ListingStatus;
import com.rominiki.waytohome.security.JwtService;
import com.rominiki.waytohome.security.UserDetailsServiceImpl;
import com.rominiki.waytohome.service.ListingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ListingService listingService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(roles = "STUDENT")
    void pending_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/listings/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void pending_asLandlord_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/listings/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approve_asAdmin_returns200() throws Exception {
        var approved = new ListingResponse(
                1L, "Cozy studio", "Near campus", BigDecimal.valueOf(750),
                "Fulda", 1, true, ListingStatus.APPROVED,
                "Test Landlord", LocalDateTime.now());

        when(listingService.approve(1L)).thenReturn(approved);

        mockMvc.perform(put("/api/admin/listings/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void approve_asStudent_returns403() throws Exception {
        mockMvc.perform(put("/api/admin/listings/1/approve"))
                .andExpect(status().isForbidden());
    }
}