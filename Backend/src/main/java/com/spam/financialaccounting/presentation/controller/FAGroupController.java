package com.spam.financialaccounting.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.spam.financialaccounting.application.usecases.CreateFAGroup;
import com.spam.financialaccounting.application.usecases.UpdateFAGroup;
import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FAGroupDTOMapper;
import com.spam.financialaccounting.presentation.dto.FAGroupDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fa-groups")
public class FAGroupController {
    private final CreateFAGroup createUseCase;
    private final UpdateFAGroup updateUseCase;

    public FAGroupController(CreateFAGroup createUseCase, UpdateFAGroup updateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
    }

    @PostMapping
    public void create(@RequestBody FAGroupDTO dto) {
        FAGroup entity = FAGroupDTOMapper.toEntity(dto);
        createUseCase.execute(entity);
    }

    @PutMapping("/{code}")
    public ResponseEntity<FAGroupDTO> update(@PathVariable String code, @Valid @RequestBody FAGroupDTO dto) {
        FAGroup entity=FAGroupDTOMapper.toEntity(dto);
        entity.setAccountCode(code);
        FAGroup updatedEntity =updateUseCase.execute(entity);
        return ResponseEntity.ok(FAGroupDTOMapper.toDTO(updatedEntity));
    }
}
