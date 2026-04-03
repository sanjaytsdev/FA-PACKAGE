package com.spam.financialaccounting.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.ArrayList;

import com.spam.financialaccounting.application.usecases.CreateFAGroup;
import com.spam.financialaccounting.application.usecases.UpdateFAGroup;
import com.spam.financialaccounting.application.usecases.GetAllFAGroup;
import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FAGroupDTOMapper;
import com.spam.financialaccounting.presentation.dto.FAGroupDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fa-groups")
public class FAGroupController {
    private final CreateFAGroup createUseCase;
    private final UpdateFAGroup updateUseCase;
    private final GetAllFAGroup getAllUseCase;

    public FAGroupController(CreateFAGroup createUseCase, UpdateFAGroup updateUseCase, GetAllFAGroup getAllUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getAllUseCase = getAllUseCase;
    }

    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody FAGroupDTO dto) {
        FAGroup entity = FAGroupDTOMapper.toEntity(dto);
        createUseCase.execute(entity);
        return ResponseEntity.status(201).body("FA Group created successfully");
    }

    @PutMapping("/{code}")
    public ResponseEntity<FAGroupDTO> update(@PathVariable String code, @Valid @RequestBody FAGroupDTO dto) {
        FAGroup entity=FAGroupDTOMapper.toEntity(dto);
        entity.setAccountCode(code);
        FAGroup updatedEntity =updateUseCase.execute(entity);
        return ResponseEntity.ok(FAGroupDTOMapper.toDTO(updatedEntity));
    }

    @GetMapping
    public ResponseEntity<List<FAGroupDTO>> getAll() {
        List<FAGroup> entities = getAllUseCase.execute();
        List<FAGroupDTO> dtos = new ArrayList<>();

        for(FAGroup entity : entities){
            FAGroupDTO dto = FAGroupDTOMapper.toDTO(entity);
            dtos.add(dto);
        }
        return ResponseEntity.ok(dtos);
    }

}
