package com.rominiki.waytohome.integration;

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
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListingModerationFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void approvedListing_appearsInPublicSearch() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String title = "Cozy studio " + unique;

        String landlordEmail = "landlord-" + unique + "@test.com";
        String adminEmail = "admin-" + unique + "@test.com";

        register(landlordEmail, "password123", "Land Lord", Role.LANDLORD);
        register(adminEmail, "password123", "Ad Min", Role.ADMIN);

        String landlordToken = login(landlordEmail, "password123");
        String adminToken = login(adminEmail, "password123");

        var req = new CreateListingRequest(
                title,
                "Near campus",
                BigDecimal.valueOf(750),
                "Fulda",
                1,
                true,
                false
        );

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long listingId = extractId(createResult);

        MvcResult beforeApprovalResult = mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(publicListingsContainId(beforeApprovalResult, listingId))
                .isFalse();

        mockMvc.perform(put("/api/admin/listings/" + listingId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        MvcResult afterApprovalResult = mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(publicListingsContainId(afterApprovalResult, listingId))
                .isTrue();

        assertThat(publicListingsContainTitle(afterApprovalResult, title))
                .isTrue();
    }

    private boolean publicListingsContainId(MvcResult result, Long listingId) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode content = json.get("content");

        for (JsonNode listing : content) {
            if (listing.get("id").asLong() == listingId) {
                return true;
            }
        }

        return false;
    }

    private boolean publicListingsContainTitle(MvcResult result, String title) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode content = json.get("content");

        for (JsonNode listing : content) {
            if (listing.get("title").asText().equals(title)) {
                return true;
            }
        }

        return false;
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