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
import com.spam.financialaccounting.application.usecases.fagroup.CreateFAGroup;
import com.spam.financialaccounting.application.usecases.fagroup.GetAllFAGroup;
import com.spam.financialaccounting.application.usecases.fagroup.GetFAGroupByCode;
import com.spam.financialaccounting.application.usecases.fagroup.GetFAGroupWithSubGroups;
import com.spam.financialaccounting.application.usecases.fagroup.UpdateFAGroup;
import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.presentation.dto.FAGroupDTO;
import com.spam.financialaccounting.presentation.dto.FAGroupWithSubGroupsDTO;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupValidationException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@WebMvcTest(FAGroupController.class)
public class FAGroupControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CreateFAGroup createFAGroup;
        @MockBean
        private UpdateFAGroup updateFAGroup;
        @MockBean
        private GetAllFAGroup getAllFAGroup;
        @MockBean
        private GetFAGroupByCode getFAGroupByCode;
        @MockBean
        private GetFAGroupWithSubGroups getFAGroupWithSubGroups;

        private FAGroup sampleEntity;
        private FAGroupDTO sampleDTO;

        @BeforeEach
        void setUp() {
                sampleEntity = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
                sampleDTO = new FAGroupDTO("01", "Asset", "0", BigDecimal.ZERO);
        }

        @Test
        @DisplayName("GET /{code} → 200 OK with FAGroup JSON when found")
        void getByCode_ShouldReturn200_WhenFound() throws Exception {
                when(getFAGroupByCode.execute("01")).thenReturn(sampleEntity);

                mockMvc.perform(get("/api/v1/fagroups/01"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accountCode").value("01"))
                                .andExpect(jsonPath("$.accountDescription").value("Asset"))
                                .andExpect(jsonPath("$.accountType").value("0"));
        }

        @Test
        @DisplayName("GET /{code} → 404 Not Found when code does not exist")
        void getByCode_ShouldReturn404_WhenNotFound() throws Exception {
                // ARRANGE — mock throws the exception
                when(getFAGroupByCode.execute("99"))
                                .thenThrow(new FAGroupNotFoundException("FAGroup not found with code: 99"));
                // ACT + ASSERT
                mockMvc.perform(get("/api/v1/fagroups/99"))
                                .andExpect(status().isNotFound()) // HTTP 404
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("FAGroup not found with code: 99"));
        }

        // GET /api/v1/fagroups

        @Test
        @DisplayName("GET / → 200 OK with list of FAGroups")
        void getAll_ShouldReturn200_WithList() throws Exception {
                // ARRANGE
                List<FAGroup> groups = Arrays.asList(
                                new FAGroup("01", "Asset", "0", BigDecimal.ZERO),
                                new FAGroup("02", "Liability", "1", BigDecimal.ZERO));
                when(getAllFAGroup.execute()).thenReturn(groups);
                // ACT + ASSERT
                mockMvc.perform(get("/api/v1/fagroups"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2)) // array has 2 items
                                .andExpect(jsonPath("$[0].accountCode").value("01"))
                                .andExpect(jsonPath("$[1].accountCode").value("02"));
        }

        @Test
        @DisplayName("GET / → 200 OK with empty array when no groups exist")
        void getAll_ShouldReturn200_WithEmptyList() throws Exception {
                when(getAllFAGroup.execute()).thenReturn(Collections.emptyList());
                mockMvc.perform(get("/api/v1/fagroups"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        // POST/api/v1/fagroups
        @Test
        @DisplayName("POST / → 201 Created with FAGroup JSON when valid input")
        void create_ShouldReturn201_WhenValidInput() throws Exception {
                when(createFAGroup.execute(any(FAGroup.class))).thenReturn(sampleEntity);

                mockMvc.perform(post("/api/v1/fagroups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleDTO)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.accountCode").value("01"))
                                .andExpect(jsonPath("$.accountDescription").value("Asset"));
        }

        @Test
        @DisplayName("POST / → 409 Conflict when group code already exists")
        void create_ShouldReturn409_WhenAlreadyExists() throws Exception {
                // ARRANGE — use case throws AlreadyExistsException
                when(createFAGroup.execute(any(FAGroup.class)))
                                .thenThrow(new FAGroupAlreadyExistsException("FA Group already exists with code: 01"));
                mockMvc.perform(post("/api/v1/fagroups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleDTO)))
                                .andExpect(status().isConflict()) // HTTP 409
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.message").value("FA Group already exists with code: 01"));
        }

        @Test
        @DisplayName("POST / → 400 Bad Request when use case throws ValidationException")
        void create_ShouldReturn400_WhenValidationFails() throws Exception {
                when(createFAGroup.execute(any(FAGroup.class)))
                                .thenThrow(new FAGroupValidationException("Account Code must be exactly 2 characters"));
                mockMvc.perform(post("/api/v1/fagroups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(sampleDTO)))
                                .andExpect(status().isBadRequest()) // HTTP 400
                                .andExpect(jsonPath("$.message")
                                                .value("Account Code must be exactly 2 characters"));
        }

        @Test
        @DisplayName("POST / → 400 Bad Request when JSON has missing required fields (DTO bean validation)")
        void create_ShouldReturn400_WhenDTOValidationFails() throws Exception {
                // Send a DTO with a blank accountCode — violates @NotBlank
                FAGroupDTO badDTO = new FAGroupDTO("", "Asset", "0", BigDecimal.ZERO);
                mockMvc.perform(post("/api/v1/fagroups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(badDTO)))
                                .andExpect(status().isBadRequest()); // HTTP 400 from @Valid
        }

        @Test
        @DisplayName("POST / → 400 Bad Request when request body is missing entirely")
        void create_ShouldReturn400_WhenBodyMissing() throws Exception {
                mockMvc.perform(post("/api/v1/fagroups")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        // PUT /api/v1/fagroups/{code}
        @Test
        @DisplayName("PUT /{code} → 200 OK with updated FAGroup JSON")
        void update_ShouldReturn200_WhenFound() throws Exception {
                // ARRANGE
                FAGroup updatedEntity = new FAGroup("01", "Updated Asset", "0", new BigDecimal("100.00"));
                when(updateFAGroup.execute(any(FAGroup.class))).thenReturn(updatedEntity);
                FAGroupDTO updateDTO = new FAGroupDTO("01", "Updated Asset", "0", new BigDecimal("100.00"));
                mockMvc.perform(put("/api/v1/fagroups/01")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isOk()) // HTTP 200
                                .andExpect(jsonPath("$.accountDescription").value("Updated Asset"))
                                .andExpect(jsonPath("$.accountCurrentBalance").value(100.00));
        }

        @Test
        @DisplayName("PUT /{code} → 404 Not Found when code does not exist")
        void update_ShouldReturn404_WhenNotFound() throws Exception {
                when(updateFAGroup.execute(any(FAGroup.class)))
                                .thenThrow(new FAGroupNotFoundException("Account Group with code 99 not found."));
                FAGroupDTO updateDTO = new FAGroupDTO("99", "Ghost", "0", BigDecimal.ZERO);
                mockMvc.perform(put("/api/v1/fagroups/99")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isNotFound()) // HTTP 404
                                .andExpect(jsonPath("$.message")
                                                .value("Account Group with code 99 not found."));
        }

        // GET /api/v1/fagroups/{code}/subgroups
        @Test
        @DisplayName("GET /{code}/subgroups → 200 OK with group and empty subgroups list")
        void getWithSubGroups_ShouldReturn200() throws Exception {
                // ARRANGE — build the DTO the use case returns
                FAGroupWithSubGroupsDTO result = new FAGroupWithSubGroupsDTO(
                                "01", "Asset", "0", BigDecimal.ZERO, Collections.emptyList());
                when(getFAGroupWithSubGroups.execute("01")).thenReturn(result);
                mockMvc.perform(get("/api/v1/fagroups/01/subgroups"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accountCode").value("01"))
                                .andExpect(jsonPath("$.subGroups").isArray())
                                .andExpect(jsonPath("$.subGroups.length()").value(0));
        }

        @Test
        @DisplayName("GET /{code}/subgroups → 404 Not Found when group code does not exist")
        void getWithSubGroups_ShouldReturn404_WhenNotFound() throws Exception {
                when(getFAGroupWithSubGroups.execute("99"))
                                .thenThrow(new FAGroupNotFoundException("Group not found with code: 99"));
                mockMvc.perform(get("/api/v1/fagroups/99/subgroups"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Group not found with code: 99"));
        }

}
