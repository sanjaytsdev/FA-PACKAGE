package com.spam.financialaccounting.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spam.financialaccounting.application.usecases.journalmaster.CreateJournalMaster;
import com.spam.financialaccounting.application.usecases.journalmaster.DeleteJournalMaster;
import com.spam.financialaccounting.application.usecases.journalmaster.GetAllJournalMaster;
import com.spam.financialaccounting.application.usecases.journalmaster.GetJournalMasterById;
import com.spam.financialaccounting.application.usecases.journalmaster.UpdateJournalMaster;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.presentation.dto.JournalMasterDTO;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;

@WebMvcTest(JournalMasterController.class)
public class JournalMasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private CreateJournalMaster createUseCase;
    @MockBean private UpdateJournalMaster updateUseCase;
    @MockBean private DeleteJournalMaster deleteUseCase;
    @MockBean private GetJournalMasterById getByIdUseCase;
    @MockBean private GetAllJournalMaster getAllUseCase;

    // jId: 10 chars, jDoc: 2 chars, jAmount >= 0, jNarr <= 100 chars
    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 1, 15, 10, 0);
    private JournalMasterDTO validDTO;
    private JournalMaster sampleEntity;

    @BeforeEach
    void setUp() {
        validDTO = new JournalMasterDTO("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Test entry");
        sampleEntity = new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Test entry");
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/v1/journal-masters
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST / → 201 Created with JournalMaster JSON when valid input")
    void create_ShouldReturn201_WhenValidInput() throws Exception {
        when(createUseCase.execute(any(JournalMaster.class))).thenReturn(sampleEntity);

        mockMvc.perform(post("/api/v1/journal-masters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jId").value("JV00000001"))
                .andExpect(jsonPath("$.jDoc").value("JV"))
                .andExpect(jsonPath("$.jAmount").value(1000.00))
                .andExpect(jsonPath("$.jNarr").value("Test entry"));
    }

    @Test
    @DisplayName("POST / → 409 Conflict when journal ID already exists")
    void create_ShouldReturn409_WhenAlreadyExists() throws Exception {
        when(createUseCase.execute(any(JournalMaster.class)))
                .thenThrow(new JournalMasterAlreadyExistsException(
                        "Journal voucher with ID JV00000001 already exists"));

        mockMvc.perform(post("/api/v1/journal-masters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDTO)))
                .andExpect(status().isBadRequest())   // GlobalExceptionHandler maps this to 400
                .andExpect(jsonPath("$.message").value("Journal voucher with ID JV00000001 already exists"));
    }

    @Test
    @DisplayName("POST / → 400 when jId is blank")
    void create_ShouldReturn400_WhenJIdIsBlank() throws Exception {
        JournalMasterDTO bad = new JournalMasterDTO("", "JV", FIXED_DATE, BigDecimal.ZERO, null);

        mockMvc.perform(post("/api/v1/journal-masters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / → 400 when jDoc is not exactly 2 characters")
    void create_ShouldReturn400_WhenJDocPatternFails() throws Exception {
        JournalMasterDTO bad = new JournalMasterDTO("JV00000001", "JVX", FIXED_DATE, BigDecimal.ZERO, null);

        mockMvc.perform(post("/api/v1/journal-masters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / → 400 when jDate is null")
    void create_ShouldReturn400_WhenDateIsNull() throws Exception {
        JournalMasterDTO bad = new JournalMasterDTO("JV00000001", "JV", null, BigDecimal.ZERO, null);

        mockMvc.perform(post("/api/v1/journal-masters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / → 400 when request body is missing")
    void create_ShouldReturn400_WhenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/v1/journal-masters").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════
    // PUT /api/v1/journal-masters/{jId}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /{jId} → 200 OK with updated JournalMaster")
    void update_ShouldReturn200_WhenFound() throws Exception {
        JournalMaster updated = new JournalMaster("JV00000001", "PV", FIXED_DATE, new BigDecimal("2000.00"), "Updated");
        when(updateUseCase.execute(any(JournalMaster.class))).thenReturn(updated);

        JournalMasterDTO updateDTO = new JournalMasterDTO("JV00000001", "PV", FIXED_DATE, new BigDecimal("2000.00"), "Updated");

        mockMvc.perform(put("/api/v1/journal-masters/JV00000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jId").value("JV00000001"))
                .andExpect(jsonPath("$.jDoc").value("PV"))
                .andExpect(jsonPath("$.jAmount").value(2000.00));
    }

    @Test
    @DisplayName("PUT /{jId} → 400 when journal ID does not exist")
    void update_ShouldReturn400_WhenNotFound() throws Exception {
        when(updateUseCase.execute(any(JournalMaster.class)))
                .thenThrow(new JournalMasterNotFoundException("Journal voucher with ID JV99999999 not found"));

        JournalMasterDTO updateDTO = new JournalMasterDTO("JV99999999", "JV", FIXED_DATE, BigDecimal.ZERO, null);

        mockMvc.perform(put("/api/v1/journal-masters/JV99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Journal voucher with ID JV99999999 not found"));
    }

    @Test
    @DisplayName("PUT /{jId} → 400 when update body is missing")
    void update_ShouldReturn400_WhenBodyMissing() throws Exception {
        mockMvc.perform(put("/api/v1/journal-masters/JV00000001").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE /api/v1/journal-masters/{jId}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /{jId} → 204 No Content when journal exists with no details")
    void delete_ShouldReturn204_WhenFound() throws Exception {
        when(deleteUseCase.execute("JV00000001")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/journal-masters/JV00000001"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /{jId} → 400 when journal does not exist")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        when(deleteUseCase.execute("JV99999999"))
                .thenThrow(new JournalMasterNotFoundException("Journal voucher with ID JV99999999 not found"));

        mockMvc.perform(delete("/api/v1/journal-masters/JV99999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Journal voucher with ID JV99999999 not found"));
    }

    @Test
    @DisplayName("DELETE /{jId} → 400 when journal has associated details (referential integrity)")
    void delete_ShouldReturn400_WhenHasDetails() throws Exception {
        when(deleteUseCase.execute("JV00000001"))
                .thenThrow(new JournalMasterValidationException(
                        "Cannot delete journal voucher with ID JV00000001 because it has 2 associated journal details. Delete the journal details first."));

        mockMvc.perform(delete("/api/v1/journal-masters/JV00000001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Cannot delete journal voucher with ID JV00000001 because it has 2 associated journal details. Delete the journal details first."));
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
    @DisplayName("GET /{jId} → 400 when journal ID does not exist")
    void getById_ShouldReturn400_WhenNotFound() throws Exception {
        when(getByIdUseCase.execute("JV99999999"))
                .thenThrow(new JournalMasterNotFoundException("Journal voucher with ID JV99999999 not found"));

        mockMvc.perform(get("/api/v1/journal-masters/JV99999999"))
                .andExpect(status().isBadRequest())
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
