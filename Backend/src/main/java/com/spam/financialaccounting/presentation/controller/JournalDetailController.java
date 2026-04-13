package com.spam.financialaccounting.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.CreateJournalDetail;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalDetailDTOMapper;
import com.spam.financialaccounting.presentation.dto.JournalDetailDTO;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/journal-details")
public class JournalDetailController {

    private final CreateJournalDetail createUseCase;

    public JournalDetailController(CreateJournalDetail createUseCase) {
        this.createUseCase = createUseCase;
    }

    @PostMapping
    public ResponseEntity<JournalDetailDTO> create(@Valid @RequestBody JournalDetailDTO dto) {
        JournalDetail entity = JournalDetailDTOMapper.toEntity(dto);
        JournalDetail created = createUseCase.execute(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalDetailDTOMapper.toDTO(created));
    }

}
