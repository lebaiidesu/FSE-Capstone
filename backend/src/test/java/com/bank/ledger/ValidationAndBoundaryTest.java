package com.bank.ledger;

import com.bank.ledger.dto.MutationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ValidationAndBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Requirement 1.A: Reject mutation amount with 5 decimal places (> 4 fractions)")
    public void testRejectInvalidFractionDigits() throws Exception {
        MutationRequest request = new MutationRequest();
        request.setAccountId(1L);
        request.setMutationAmount(new BigDecimal("100.12345")); // 5 decimals violates @Digits(fraction=4)
        request.setOperation("DEBIT");

        mockMvc.perform(post("/api/v1/ledger/mutate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.paypink.ph/errors/validation-error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams[0].name").value("mutationAmount"));
    }

    @Test
    @DisplayName("Requirement 1.A: Reject negative mutation amounts at perimeter (@Positive)")
    public void testRejectNegativeMutationAmount() throws Exception {
        MutationRequest request = new MutationRequest();
        request.setAccountId(1L);
        request.setMutationAmount(new BigDecimal("-50.0000")); // Negative violates @Positive
        request.setOperation("DEBIT");

        mockMvc.perform(post("/api/v1/ledger/mutate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.paypink.ph/errors/validation-error"))
                .andExpect(jsonPath("$.invalidParams[0].name").value("mutationAmount"));
    }

    @Test
    @DisplayName("Requirement 1.A: Intercept malformed JSON schemas and return RFC-7807 Problem Details")
    public void testInterceptMalformedJson() throws Exception {
        String malformedJson = "{ \"accountId\": \"INVALID_STRING\", \"mutationAmount\": \"NOT_A_NUMBER\" }";

        mockMvc.perform(post("/api/v1/ledger/mutate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.paypink.ph/errors/malformed-payload"))
                .andExpect(jsonPath("$.title").value("Malformed JSON Schema"));
    }
}
