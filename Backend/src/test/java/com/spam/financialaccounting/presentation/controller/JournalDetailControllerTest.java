package com.spam.financialaccounting.presentation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.spam.financialaccounting.application.usecases.journaldetail.GetAllJournalDetails;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetail;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetailsByAccountCode;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetailsByJournalId;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;

@WebMvcTest(JournalDetailController.class)
public class JournalDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private GetJournalDetail getUseCase;
    @MockBean private GetAllJournalDetails getAllUseCase;
    @MockBean private GetJournalDetailsByJournalId getByJournalIdUseCase;
    @MockBean private GetJournalDetailsByAccountCode getByAccountCodeUseCase;

    private JournalDetail sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEntity = new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("500.00"));
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/v1/journal-details/{jId}/{jCode}/{jDrCr}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /{jId}/{jCode}/{jDrCr} → 200 OK with JournalDetail JSON when found")
    void getByCompositeKey_ShouldReturn200_WhenFound() throws Exception {
        when(getUseCase.execute("V001000001", "SG001", "DR")).thenReturn(sampleEntity);

        mockMvc.perform(get("/api/v1/journal-details/V001000001/SG001/DR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jId").value("V001000001"))
                .andExpect(jsonPath("$.jCode").value("SG001"))
                .andExpect(jsonPath("$.jDrCr").value("DR"))
                .andExpect(jsonPath("$.jAmount").value(500.00));
    }

    @Test
    @DisplayName("GET /{jId}/{jCode}/{jDrCr} → 404 Not Found when composite key does not exist")
    void getByCompositeKey_ShouldReturn404_WhenNotFound() throws Exception {
        when(getUseCase.execute("V999999999", "SG999", "DR"))
                .thenThrow(new JournalDetailNotFoundException(
                        "Journal detail not found for voucher V999999999, account SG999, type DR"));

        mockMvc.perform(get("/api/v1/journal-details/V999999999/SG999/DR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Journal detail not found for voucher V999999999, account SG999, type DR"));
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/v1/journal-details
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET / → 200 OK with list of all JournalDetails")
    void getAll_ShouldReturn200_WithList() throws Exception {
        List<JournalDetail> details = Arrays.asList(
                new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("500.00")),
                new JournalDetail("V001000001", "SG002", "CR", new BigDecimal("500.00")));
        when(getAllUseCase.execute()).thenReturn(details);

        mockMvc.perform(get("/api/v1/journal-details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jId").value("V001000001"))
                .andExpect(jsonPath("$[0].jDrCr").value("DR"))
                .andExpect(jsonPath("$[1].jDrCr").value("CR"));
    }

    @Test
    @DisplayName("GET / → 200 OK with empty array when no JournalDetails exist")
    void getAll_ShouldReturn200_WithEmptyArray() throws Exception {
        when(getAllUseCase.execute()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/journal-details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/v1/journal-details/journal/{jId}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /journal/{jId} → 200 OK with all details for that voucher ID")
    void getByJournalId_ShouldReturn200_WithDetails() throws Exception {
        List<JournalDetail> details = Arrays.asList(
                new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("500.00")),
                new JournalDetail("V001000001", "SG002", "CR", new BigDecimal("500.00")));
        when(getByJournalIdUseCase.execute("V001000001")).thenReturn(details);

        mockMvc.perform(get("/api/v1/journal-details/journal/V001000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jCode").value("SG001"))
                .andExpect(jsonPath("$[1].jCode").value("SG002"));
    }

    @Test
    @DisplayName("GET /journal/{jId} → 404 Not Found when journal voucher ID does not exist")
    void getByJournalId_ShouldReturn404_WhenVoucherNotFound() throws Exception {
        when(getByJournalIdUseCase.execute("V999999999"))
                .thenThrow(new JournalMasterNotFoundException(
                        "Journal voucher with ID V999999999 does not exist"));

        mockMvc.perform(get("/api/v1/journal-details/journal/V999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Journal voucher with ID V999999999 does not exist"));
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/v1/journal-details/account/{jCode}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /account/{jCode} → 200 OK with all details for that account code")
    void getByAccountCode_ShouldReturn200_WithDetails() throws Exception {
        List<JournalDetail> details = Arrays.asList(
                new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("500.00")),
                new JournalDetail("V002000001", "SG001", "CR", new BigDecimal("200.00")));
        when(getByAccountCodeUseCase.execute("SG001")).thenReturn(details);

        mockMvc.perform(get("/api/v1/journal-details/account/SG001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jCode").value("SG001"))
                .andExpect(jsonPath("$[1].jCode").value("SG001"));
    }

    @Test
    @DisplayName("GET /account/{jCode} → 404 Not Found when ledger account code does not exist")
    void getByAccountCode_ShouldReturn404_WhenAccountNotFound() throws Exception {
        when(getByAccountCodeUseCase.execute("XX999"))
                .thenThrow(new FASubGroupNotFoundException(
                        "Ledger account with code XX999 does not exist"));

        mockMvc.perform(get("/api/v1/journal-details/account/XX999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Ledger account with code XX999 does not exist"));
    }
}
