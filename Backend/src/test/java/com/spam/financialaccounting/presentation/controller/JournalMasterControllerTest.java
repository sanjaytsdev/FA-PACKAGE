package com.spam.financialaccounting.presentation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import com.spam.financialaccounting.application.usecases.journalmaster.GetAllJournalMaster;
import com.spam.financialaccounting.application.usecases.journalmaster.GetJournalMasterById;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;

@WebMvcTest(JournalMasterController.class)
public class JournalMasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private GetJournalMasterById getByIdUseCase;
    @MockBean private GetAllJournalMaster getAllUseCase;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 1, 15, 10, 0);
    private JournalMaster sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEntity = new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Test entry");
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/v1/journal-masters/{jId}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /{jId} → 200 OK with JournalMaster JSON when found")
    void getById_ShouldReturn200_WhenFound() throws Exception {
        when(getByIdUseCase.execute("JV00000001")).thenReturn(sampleEntity);

        mockMvc.perform(get("/api/v1/journal-masters/JV00000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jId").value("JV00000001"))
                .andExpect(jsonPath("$.jDoc").value("JV"))
                .andExpect(jsonPath("$.jAmount").value(1000.00))
                .andExpect(jsonPath("$.jNarr").value("Test entry"));
    }

    @Test
    @DisplayName("GET /{jId} → 404 when journal ID does not exist")
    void getById_ShouldReturn404_WhenNotFound() throws Exception {
        when(getByIdUseCase.execute("JV99999999"))
                .thenThrow(new JournalMasterNotFoundException("Journal voucher with ID JV99999999 not found"));

        mockMvc.perform(get("/api/v1/journal-masters/JV99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Journal voucher with ID JV99999999 not found"));
    }

    // ═══════════════════════════════════════════════════════════
    // GET /api/v1/journal-masters
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET / → 200 OK with list of all JournalMasters")
    void getAll_ShouldReturn200_WithList() throws Exception {
        List<JournalMaster> data = Arrays.asList(
                new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "A"),
                new JournalMaster("JV00000002", "PV", FIXED_DATE, new BigDecimal("500.00"), "B"));
        when(getAllUseCase.execute()).thenReturn(data);

        mockMvc.perform(get("/api/v1/journal-masters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].jId").value("JV00000001"))
                .andExpect(jsonPath("$[1].jId").value("JV00000002"));
    }

    @Test
    @DisplayName("GET / → 200 OK with empty array when no records exist")
    void getAll_ShouldReturn200_WithEmptyArray() throws Exception {
        when(getAllUseCase.execute()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/journal-masters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
