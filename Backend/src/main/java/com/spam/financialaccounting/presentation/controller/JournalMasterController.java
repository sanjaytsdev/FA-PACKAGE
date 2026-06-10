package com.spam.financialaccounting.presentation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.journalmaster.GetAllJournalMaster;
import com.spam.financialaccounting.application.usecases.journalmaster.GetJournalMasterById;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalMasterDTOMapper;
import com.spam.financialaccounting.presentation.dto.JournalMasterDTO;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;




/**
 * Read-only access to journal voucher headers.
 *
 * No create/update/delete here, on purpose: a header means nothing without its
 * balanced lines, so all posting and removal goes through
 * {@link JournalVoucherController} (/api/v1/journal-vouchers), which keeps the
 * double-entry balance and does it atomically.
 */
@RestController
@RequestMapping("/api/v1/journal-masters")
public class JournalMasterController {

    private final GetJournalMasterById getByIdUseCase;
    private final GetAllJournalMaster getAllUseCase;

    public JournalMasterController(
        GetJournalMasterById getByIdUseCase,
        GetAllJournalMaster getAllUseCase

    ){
        this.getByIdUseCase = getByIdUseCase;
        this.getAllUseCase = getAllUseCase;
    }

    @GetMapping("/{jId}")
    public ResponseEntity<JournalMasterDTO> getById(@PathVariable String jId) {
        JournalMaster journalMaster = getByIdUseCase.execute(jId);
        return ResponseEntity.ok(JournalMasterDTOMapper.toDTO(journalMaster));
    }

    @GetMapping
    public ResponseEntity<List<JournalMasterDTO>> getAll() {
        List<JournalMaster> journalMasters = getAllUseCase.execute();
        List<JournalMasterDTO> dtos = journalMasters.stream()
            .map(JournalMasterDTOMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }



}
