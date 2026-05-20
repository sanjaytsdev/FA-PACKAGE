package com.spam.financialaccounting.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spam.financialaccounting.application.usecases.journaldetail.CreateJournalDetail;
import com.spam.financialaccounting.application.usecases.journaldetail.DeleteJournalDetail;
import com.spam.financialaccounting.application.usecases.journaldetail.GetAllJournalDetails;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetail;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetailsByAccountCode;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetailsByJournalId;
import com.spam.financialaccounting.application.usecases.journaldetail.UpdateJournalDetail;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.presentation.dto.JournalDetailDTO;
import com.spam.financialaccounting.presentation.dto.JournalDetailUpdateRequestDTO;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailValidationException;

@WebMvcTest(JournalDetailController.class)
public class JournalDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private CreateJournalDetail createUseCase;
    @MockBean private GetJournalDetail getUseCase;
    @MockBean private GetAllJournalDetails getAllUseCase;
    @MockBean private GetJournalDetailsByJournalId getByJournalIdUseCase;
    @MockBean private GetJournalDetailsByAccountCode getByAccountCodeUseCase;
    @MockBean private UpdateJournalDetail updateUseCase;
    @MockBean private DeleteJournalDetail deleteUseCase;

    // Valid DTO — matches all @NotBlank / @Pattern / @DecimalMin constraints
    private JournalDetailDTO validDTO;
    private JournalDetail sampleEntity;

    @BeforeEach
    void setUp() {
        // jId: exactly 10 chars, jCode: exactly 5 chars, jDrCr: "DR" or "CR", jAmount > 0
        validDTO    = new JournalDetailDTO("V001000001", "SG001", "DR", new BigDecimal("500.00"));
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
    @DisplayName("GET /journal/{jId} → 400 Bad Request when journal voucher ID does not exist")
    void getByJournalId_ShouldReturn400_WhenVoucherNotFound() throws Exception {
        when(getByJournalIdUseCase.execute("V999999999"))
                .thenThrow(new JournalDetailValidationException(
                        "Journal voucher with ID V999999999 does not exist"));

        mockMvc.perform(get("/api/v1/journal-details/journal/V999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
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
    @DisplayName("GET /account/{jCode} → 400 Bad Request when ledger account code does not exist")
    void getByAccountCode_ShouldReturn400_WhenAccountNotFound() throws Exception {
        when(getByAccountCodeUseCase.execute("XX999"))
                .thenThrow(new JournalDetailValidationException(
                        "Ledger account with code XX999 does not exist"));

        mockMvc.perform(get("/api/v1/journal-details/account/XX999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Ledger account with code XX999 does not exist"));
    }

    // ═══════════════════════════════════════════════════════════
    // POST /api/v1/journal-details
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST / → 201 Created with JournalDetail JSON when valid input")
    void create_ShouldReturn201_WhenValidInput() throws Exception {
        when(createUseCase.execute(any(JournalDetail.class))).thenReturn(sampleEntity);

        mockMvc.perform(post("/api/v1/journal-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jId").value("V001000001"))
                .andExpect(jsonPath("$.jCode").value("SG001"))
                .andExpect(jsonPath("$.jDrCr").value("DR"))
                .andExpect(jsonPath("$.jAmount").value(500.00));
    }

    @Test
    @DisplayName("POST / → 409 Conflict when composite key already exists")
    void create_ShouldReturn409_WhenAlreadyExists() throws Exception {
        when(createUseCase.execute(any(JournalDetail.class)))
                .thenThrow(new JournalDetailAlreadyExistsException(
                        "Journal detail already exists for voucher V001000001, account SG001, type DR"));

        mockMvc.perform(post("/api/v1/journal-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Journal detail already exists for voucher V001000001, account SG001, type DR"));
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when JournalMaster or FASubGroup does not exist (ValidationException)")
    void create_ShouldReturn400_WhenValidationFails() throws Exception {
        when(createUseCase.execute(any(JournalDetail.class)))
                .thenThrow(new JournalDetailValidationException(
                        "Journal voucher with ID V001000001 does not exist"));

        mockMvc.perform(post("/api/v1/journal-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Journal voucher with ID V001000001 does not exist"));
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when amount is zero or negative (IllegalArgumentException)")
    void create_ShouldReturn400_WhenAmountInvalid() throws Exception {
        when(createUseCase.execute(any(JournalDetail.class)))
                .thenThrow(new IllegalArgumentException("Amount must be greater than zero"));

        mockMvc.perform(post("/api/v1/journal-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be greater than zero"));
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when DTO fails @NotBlank on jId")
    void create_ShouldReturn400_WhenJIdIsBlank() throws Exception {
        JournalDetailDTO badDTO = new JournalDetailDTO("", "SG001", "DR", new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/journal-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when DTO fails @Pattern on jDrCr (not DR or CR)")
    void create_ShouldReturn400_WhenDrCrPatternFails() throws Exception {
        JournalDetailDTO badDTO = new JournalDetailDTO("V001000001", "SG001", "XX", new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/journal-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when jCode is not exactly 5 characters")
    void create_ShouldReturn400_WhenJCodePatternFails() throws Exception {
        JournalDetailDTO badDTO = new JournalDetailDTO("V001000001", "SG", "DR", new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/journal-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when request body is missing entirely")
    void create_ShouldReturn400_WhenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/v1/journal-details")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════
    // PUT /api/v1/journal-details/{jId}/{jCode}/{jDrCr}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /{jId}/{jCode}/{jDrCr} → 200 OK with updated JournalDetail")
    void update_ShouldReturn200_WhenFound() throws Exception {
        JournalDetail updatedEntity = new JournalDetail("V001000001", "SG001", "DR", new BigDecimal("999.00"));
        when(updateUseCase.execute(
                eq("V001000001"), eq("SG001"), eq("DR"), any(BigDecimal.class)))
                .thenReturn(updatedEntity);

        JournalDetailUpdateRequestDTO updateDTO = new JournalDetailUpdateRequestDTO(new BigDecimal("999.00"));

        mockMvc.perform(put("/api/v1/journal-details/V001000001/SG001/DR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jId").value("V001000001"))
                .andExpect(jsonPath("$.jAmount").value(999.00));
    }

    @Test
    @DisplayName("PUT /{jId}/{jCode}/{jDrCr} → 404 Not Found when composite key does not exist")
    void update_ShouldReturn404_WhenNotFound() throws Exception {
        when(updateUseCase.execute(
                eq("V999999999"), eq("SG999"), eq("DR"), any(BigDecimal.class)))
                .thenThrow(new JournalDetailNotFoundException(
                        "Journal detail not found for voucher V999999999, account SG999, type DR"));

        JournalDetailUpdateRequestDTO updateDTO = new JournalDetailUpdateRequestDTO(new BigDecimal("100.00"));

        mockMvc.perform(put("/api/v1/journal-details/V999999999/SG999/DR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Journal detail not found for voucher V999999999, account SG999, type DR"));
    }

    @Test
    @DisplayName("PUT /{jId}/{jCode}/{jDrCr} → 400 Bad Request when amount in body is zero")
    void update_ShouldReturn400_WhenAmountIsZeroInDTO() throws Exception {
        // @DecimalMin(0.001) on the DTO catches this before the use case is called
        JournalDetailUpdateRequestDTO badDTO = new JournalDetailUpdateRequestDTO(BigDecimal.ZERO);

        mockMvc.perform(put("/api/v1/journal-details/V001000001/SG001/DR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /{jId}/{jCode}/{jDrCr} → 400 Bad Request when update body is missing")
    void update_ShouldReturn400_WhenBodyMissing() throws Exception {
        mockMvc.perform(put("/api/v1/journal-details/V001000001/SG001/DR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE /api/v1/journal-details/{jId}/{jCode}/{jDrCr}
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /{jId}/{jCode}/{jDrCr} → 204 No Content when detail exists")
    void delete_ShouldReturn204_WhenFound() throws Exception {
        when(deleteUseCase.execute("V001000001", "SG001", "DR")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/journal-details/V001000001/SG001/DR"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /{jId}/{jCode}/{jDrCr} → 404 Not Found when composite key does not exist")
    void delete_ShouldReturn404_WhenNotFound() throws Exception {
        when(deleteUseCase.execute("V999999999", "SG999", "DR"))
                .thenThrow(new JournalDetailNotFoundException(
                        "Journal detail not found for voucher V999999999, account SG999, type DR"));

        mockMvc.perform(delete("/api/v1/journal-details/V999999999/SG999/DR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Journal detail not found for voucher V999999999, account SG999, type DR"));
    }
}
