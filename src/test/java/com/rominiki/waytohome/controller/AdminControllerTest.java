package com.rominiki.waytohome.controller;

import com.rominiki.waytohome.config.SecurityConfig;
import com.rominiki.waytohome.service.ListingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ListingService listingService;

    @Test
    @WithMockUser(roles = "STUDENT")
    void pending_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/listings/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approve_asAdmin_returns200() throws Exception {
        when(listingService.approve(1L))
                .thenReturn(null);
        mockMvc.perform(put("/api/admin/listings/1/approve"))
                .andExpect(status().isOk());
    }
}
