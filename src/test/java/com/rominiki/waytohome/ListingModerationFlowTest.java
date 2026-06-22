package com.rominiki.waytohome;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rominiki.waytohome.dto.CreateListingRequest;
import com.rominiki.waytohome.dto.LoginRequest;
import com.rominiki.waytohome.dto.RegisterRequest;
import com.rominiki.waytohome.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListingModerationFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void approvedListing_appearsInPublicSearch() throws Exception {
        register("landlord@test.com", "password123", "Land Lord", Role.LANDLORD);
        register("admin@test.com", "password123", "Ad Min", Role.ADMIN);

        String landlordToken = login("landlord@test.com", "password123");
        String adminToken = login("admin@test.com", "password123");

        var req = new CreateListingRequest(
                "Cozy studio", "Near campus", BigDecimal.valueOf(750),
                "Fulda", 1, true, false);

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long listingId = extractId(createResult);

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(put("/api/admin/listings/" + listingId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.content[0].title").value("Cozy studio"));
    }


    private void register(String email, String password, String fullName, Role role)
            throws Exception {
        var req = new RegisterRequest(email, password, fullName, role);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        var req = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private Long extractId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}