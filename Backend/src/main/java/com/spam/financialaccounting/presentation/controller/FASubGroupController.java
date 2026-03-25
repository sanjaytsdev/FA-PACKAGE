package com.spam.financialaccounting.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.CreateFASubGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FASubGroupDTOMapper;
import com.spam.financialaccounting.presentation.dto.FASubGroupDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ledger-accounts")
public class FASubGroupController {

    private final CreateFASubGroup createUseCase;

    public FASubGroupController(CreateFASubGroup createUseCase) {
        this.createUseCase=createUseCase;
    }

    @PostMapping
    public ResponseEntity<FASubGroupDTO> create(@Valid @RequestBody FASubGroupDTO dto) {
        FASubGroup entity=FASubGroupDTOMapper.toEntity(dto);
        FASubGroup created = createUseCase.execute(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(FASubGroupDTOMapper.toDTO(created));
        
    }
    
}
