package com.spam.financialaccounting.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.spam.financialaccounting.application.usecases.CreateFAGroup;
import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FAGroupDTOMapper;
import com.spam.financialaccounting.presentation.dto.FAGroupDTO;

@RestController
public class FAGroupController {
    private final CreateFAGroup createUseCase;

    public FAGroupController(CreateFAGroup createUseCase) {
        this.createUseCase = createUseCase;
    }

    @PostMapping
    public void create(@RequestBody FAGroupDTO dto) {
        FAGroup entity = FAGroupDTOMapper.toEntity(dto);
        createUseCase.execute(entity);
    }
}
