package com.spam.financialaccounting.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.ArrayList;

import com.spam.financialaccounting.application.usecases.CreateFASubGroup;
import com.spam.financialaccounting.application.usecases.DeleteFASubGroup;
import com.spam.financialaccounting.application.usecases.GetAllFASubGroup;
import com.spam.financialaccounting.application.usecases.GetFASubGroupByCode;
import com.spam.financialaccounting.application.usecases.UpdateFASubGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FASubGroupDTOMapper;
import com.spam.financialaccounting.presentation.dto.FASubGroupDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ledger-accounts")
public class FASubGroupController {

    private final CreateFASubGroup createUseCase;
    private final GetAllFASubGroup getAllUseCase;
    private final GetFASubGroupByCode getByCodeUseCase;
    private final UpdateFASubGroup updateUseCase;
    private final DeleteFASubGroup deleteUseCase;

    public FASubGroupController(CreateFASubGroup createUseCase,
                                GetAllFASubGroup getAllUseCase,
                                GetFASubGroupByCode getByCodeUseCase,
                                UpdateFASubGroup updateUseCase,
                                DeleteFASubGroup deleteUseCase
    ) {
        this.createUseCase=createUseCase;
        this.getAllUseCase=getAllUseCase;
        this.getByCodeUseCase=getByCodeUseCase;
        this.updateUseCase=updateUseCase;
        this.deleteUseCase=deleteUseCase;
    }

    @PostMapping
    public ResponseEntity<FASubGroupDTO> create(@Valid @RequestBody FASubGroupDTO dto) {
        FASubGroup entity=FASubGroupDTOMapper.toEntity(dto);
        FASubGroup created = createUseCase.execute(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(FASubGroupDTOMapper.toDTO(created));
    }
    
    @GetMapping
    public ResponseEntity<List<FASubGroupDTO>> getAll() {
        List<FASubGroup> entities = getAllUseCase.execute();
        List<FASubGroupDTO> dtos = new ArrayList<>();
        for(FASubGroup entity : entities){
            dtos.add(FASubGroupDTOMapper.toDTO(entity));
        }
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{code}")
    public ResponseEntity<FASubGroupDTO> getByCode(@PathVariable String code) {
        FASubGroup entity = getByCodeUseCase.execute(code);
        return ResponseEntity.ok(FASubGroupDTOMapper.toDTO(entity));
    }

    @PutMapping("/{code}")
    public ResponseEntity<FASubGroupDTO> update(@PathVariable String code, @Valid @RequestBody FASubGroupDTO dto) {
        FASubGroup entity = FASubGroupDTOMapper.toEntity(dto);
        entity.setSCode(code);
        FASubGroup updatedEntity = updateUseCase.execute(entity);
        return ResponseEntity.ok(FASubGroupDTOMapper.toDTO(updatedEntity));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        deleteUseCase.execute(code);
        return ResponseEntity.noContent().build();
    }

}
