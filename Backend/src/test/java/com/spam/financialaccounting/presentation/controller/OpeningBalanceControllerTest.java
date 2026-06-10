package com.spam.financialaccounting.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.spam.financialaccounting.application.usecases.openingbalance.ImportOpeningBalances;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.presentation.exception.openingbalance.OpeningBalanceAlreadyInitializedException;
import com.spam.financialaccounting.presentation.exception.openingbalance.OpeningBalanceImbalanceException;

@WebMvcTest(OpeningBalanceController.class)
public class OpeningBalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportOpeningBalances importOpeningBalances;

    private static final String BALANCED_BODY = """
            {"narration":"Opening Balance","lines":[
              {"accountCode":"10001","drCr":"DR","amount":1000},
              {"accountCode":"30001","drCr":"CR","amount":1000}]}""";

    @Test
    @DisplayName("POST /import → 201 with the created Opening Balance voucher when balanced")
    void importsBalancedOpeningBalances() throws Exception {
        JournalMaster voucher = new JournalMaster("JV20260001", "OB",
                LocalDateTime.of(2026, 1, 1, 0, 0), new BigDecimal("1000.00"), "Opening Balance");
        when(importOpeningBalances.execute(any(), any(), any())).thenReturn(voucher);

        mockMvc.perform(post("/api/v1/opening-balances/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BALANCED_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jId").value("JV20260001"))
                .andExpect(jsonPath("$.jDoc").value("OB"))
                .andExpect(jsonPath("$.jAmount").value(1000.00));
    }

    @Test
    @DisplayName("POST /import → 422 with an accounting error when the set is unbalanced")
    void rejectsUnbalancedOpeningBalances() throws Exception {
        when(importOpeningBalances.execute(any(), any(), any()))
                .thenThrow(new OpeningBalanceImbalanceException(new BigDecimal("5000"), new BigDecimal("4000")));

        mockMvc.perform(post("/api/v1/opening-balances/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BALANCED_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("out of balance")));
    }

    @Test
    @DisplayName("POST /import → 409 when opening balances were already imported")
    void rejectsReinitialization() throws Exception {
        when(importOpeningBalances.execute(any(), any(), any()))
                .thenThrow(new OpeningBalanceAlreadyInitializedException("Opening balances have already been imported"));

        mockMvc.perform(post("/api/v1/opening-balances/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BALANCED_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /import → 400 when fewer than two lines are supplied (bean validation)")
    void rejectsTooFewLines() throws Exception {
        String oneLine = """
                {"lines":[{"accountCode":"10001","drCr":"DR","amount":1000}]}""";

        mockMvc.perform(post("/api/v1/opening-balances/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(oneLine))
                .andExpect(status().isBadRequest());

        verify(importOpeningBalances, never()).execute(any(), any(), any());
    }
}
