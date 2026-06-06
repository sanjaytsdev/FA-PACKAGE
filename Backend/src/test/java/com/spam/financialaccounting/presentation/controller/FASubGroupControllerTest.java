package com.spam.financialaccounting.presentation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spam.financialaccounting.application.usecases.fasubgroup.CreateFASubGroup;
import com.spam.financialaccounting.application.usecases.fasubgroup.DeleteFASubGroup;
import com.spam.financialaccounting.application.usecases.fasubgroup.GetAllFASubGroup;
import com.spam.financialaccounting.application.usecases.fasubgroup.GetFASubGroupByCode;
import com.spam.financialaccounting.application.usecases.fasubgroup.UpdateFASubGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.presentation.dto.FASubGroupDTO;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@WebMvcTest(FASubGroupController.class)
public class FASubGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateFASubGroup createFASubGroup;
    @MockBean
    private GetAllFASubGroup getAllFASubGroup;
    @MockBean
    private GetFASubGroupByCode getFASubGroupByCode;
    @MockBean
    private UpdateFASubGroup updateFASubGroup;
    @MockBean
    private DeleteFASubGroup deleteFASubGroup;

    private FASubGroup sampleEntity;
    private FASubGroupDTO sampleDTO;

    @BeforeEach
    void setUp() {
        sampleEntity = new FASubGroup("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");
        sampleDTO = new FASubGroupDTO("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");
    }

    // GET /api/v1/ledger-accounts/{code}
    @Test
    @DisplayName("GET /{code} → 200 OK with FASubGroup JSON when found")
    void getByCode_ShouldReturn200_WhenFound() throws Exception {
        when(getFASubGroupByCode.execute("10001")).thenReturn(sampleEntity);

        mockMvc.perform(get("/api/v1/ledger-accounts/10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sCode").value("10001"))
                .andExpect(jsonPath("$.sDesc").value("Cash"))
                .andExpect(jsonPath("$.aCode").value("01"))
                .andExpect(jsonPath("$.sType").value("00"))
                .andExpect(jsonPath("$.sOpbal").value(1000.00))
                .andExpect(jsonPath("$.sDrCr").value("DR"))
                .andExpect(jsonPath("$.sFlag").value("T"));
    }

    @Test
    @DisplayName("GET /{code} → 404 Not Found when code does not exist")
    void getByCode_ShouldReturn404_WhenNotFound() throws Exception {
        when(getFASubGroupByCode.execute("99999"))
                .thenThrow(new FASubGroupNotFoundException("FASubGroup not found with code: 99999"));

        mockMvc.perform(get("/api/v1/ledger-accounts/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("FASubGroup not found with code: 99999"));
    }

    // GET /api/v1/ledger-accounts
    @Test
    @DisplayName("GET / → 200 OK with list of FASubGroups")
    void getAll_ShouldReturn200_WithList() throws Exception {
        List<FASubGroup> list = Arrays.asList(
                new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", "T"),
                new FASubGroup("10002", "Bank", "01", "00", BigDecimal.ZERO, "DR", "T")
        );
        when(getAllFASubGroup.execute()).thenReturn(list);

        mockMvc.perform(get("/api/v1/ledger-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sCode").value("10001"))
                .andExpect(jsonPath("$[1].sCode").value("10002"));
    }

    @Test
    @DisplayName("GET / → 200 OK with empty array when none exist")
    void getAll_ShouldReturn200_WithEmptyList() throws Exception {
        when(getAllFASubGroup.execute()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/ledger-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // POST /api/v1/ledger-accounts
    @Test
    @DisplayName("POST / → 201 Created with FASubGroup JSON when valid input")
    void create_ShouldReturn201_WhenValidInput() throws Exception {
        when(createFASubGroup.execute(any(FASubGroup.class))).thenReturn(sampleEntity);

        mockMvc.perform(post("/api/v1/ledger-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sCode").value("10001"))
                .andExpect(jsonPath("$.sDesc").value("Cash"));
    }

    @Test
    @DisplayName("POST / → 409 Conflict when group code already exists")
    void create_ShouldReturn409_WhenAlreadyExists() throws Exception {
        when(createFASubGroup.execute(any(FASubGroup.class)))
                .thenThrow(new FASubGroupAlreadyExistsException("Ledger Account already exist with code: 10001"));

        mockMvc.perform(post("/api/v1/ledger-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ledger Account already exist with code: 10001"));
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when validation fails")
    void create_ShouldReturn400_WhenValidationFails() throws Exception {
        when(createFASubGroup.execute(any(FASubGroup.class)))
                .thenThrow(new FASubGroupValidationException("Invalid details"));

        mockMvc.perform(post("/api/v1/ledger-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid details"));
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when DTO field validation fails")
    void create_ShouldReturn400_WhenDTOFieldValidationFails() throws Exception {
        FASubGroupDTO badDTO = new FASubGroupDTO("", "Cash", "01", "00", BigDecimal.ZERO, "DR", "T");

        mockMvc.perform(post("/api/v1/ledger-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST / → 400 Bad Request when request body is missing")
    void create_ShouldReturn400_WhenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/v1/ledger-accounts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // PUT /api/v1/ledger-accounts/{code}
    @Test
    @DisplayName("PUT /{code} → 200 OK with updated FASubGroup JSON")
    void update_ShouldReturn200_WhenValid() throws Exception {
        FASubGroup updatedEntity = new FASubGroup("10001", "Petty Cash", "01", "00", new BigDecimal("1200.00"), "DR", "T");
        when(updateFASubGroup.execute(any(FASubGroup.class))).thenReturn(updatedEntity);

        FASubGroupDTO updateDTO = new FASubGroupDTO("10001", "Petty Cash", "01", "00", new BigDecimal("1200.00"), "DR", "T");

        mockMvc.perform(put("/api/v1/ledger-accounts/10001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sDesc").value("Petty Cash"))
                .andExpect(jsonPath("$.sOpbal").value(1200.00));
    }

    @Test
    @DisplayName("PUT /{code} → 404 Not Found when code does not exist")
    void update_ShouldReturn404_WhenNotFound() throws Exception {
        when(updateFASubGroup.execute(any(FASubGroup.class)))
                .thenThrow(new FASubGroupNotFoundException("Ledger Account with code 99999 not found."));

        FASubGroupDTO updateDTO = new FASubGroupDTO("99999", "Ghost", "01", "00", BigDecimal.ZERO, "DR", "T");

        mockMvc.perform(put("/api/v1/ledger-accounts/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ledger Account with code 99999 not found."));
    }

    // DELETE /api/v1/ledger-accounts/{code}
    @Test
    @DisplayName("DELETE /{code} → 204 No Content on success")
    void delete_ShouldReturn204_OnSuccess() throws Exception {
        when(deleteFASubGroup.execute("10001")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/ledger-accounts/10001"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /{code} → 404 Not Found when ledger account does not exist")
    void delete_ShouldReturn404_WhenNotFound() throws Exception {
        when(deleteFASubGroup.execute("99999"))
                .thenThrow(new FASubGroupNotFoundException("Ledger Account with code 99999 not found"));

        mockMvc.perform(delete("/api/v1/ledger-accounts/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ledger Account with code 99999 not found"));
    }

    @Test
    @DisplayName("DELETE /{code} → 400 Bad Request when ledger account has journal entries")
    void delete_ShouldReturn400_WhenHasJournalEntries() throws Exception {
        when(deleteFASubGroup.execute("10001"))
                .thenThrow(new FASubGroupValidationException("Cannot delete ledger account with existing journal entries."));

        mockMvc.perform(delete("/api/v1/ledger-accounts/10001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete ledger account with existing journal entries."));
    }
}
