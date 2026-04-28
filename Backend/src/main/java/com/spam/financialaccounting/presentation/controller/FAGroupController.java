package com.spam.financialaccounting.presentation.controller;

import org.springframework.http.HttpStatus;
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

import com.spam.financialaccounting.application.usecases.fagroup.CreateFAGroup;
import com.spam.financialaccounting.application.usecases.fagroup.GetAllFAGroup;
import com.spam.financialaccounting.application.usecases.fagroup.GetFAGroupByCode;
import com.spam.financialaccounting.application.usecases.fagroup.GetFAGroupWithSubGroups;
import com.spam.financialaccounting.application.usecases.fagroup.UpdateFAGroup;
import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FAGroupDTOMapper;
import com.spam.financialaccounting.presentation.dto.FAGroupDTO;
import com.spam.financialaccounting.presentation.dto.FAGroupWithSubGroupsDTO;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/v1/fagroups")
public class FAGroupController {

    private final CreateFAGroup createUseCase;
    private final UpdateFAGroup updateUseCase;
    private final GetAllFAGroup getAllUseCase;
    private final GetFAGroupByCode getByCodeUseCase;
    private final GetFAGroupWithSubGroups getFAGroupWithSubGroups;

    // Single Constructor with all dependencies
    public FAGroupController(CreateFAGroup createUseCase, 
                             UpdateFAGroup updateUseCase, 
                             GetAllFAGroup getAllUseCase,
                             GetFAGroupByCode getByCodeUseCase, 
                             GetFAGroupWithSubGroups getFAGroupWithSubGroups) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getAllUseCase = getAllUseCase;
        this.getByCodeUseCase = getByCodeUseCase;
        this.getFAGroupWithSubGroups = getFAGroupWithSubGroups;
    }

    @GetMapping("/{code}")
    public ResponseEntity<FAGroupDTO> getByCode(@PathVariable String code) {
        FAGroup group= getByCodeUseCase.execute(code);
        return ResponseEntity.ok(FAGroupDTOMapper.toDTO(group));
    }


    @PostMapping
    public ResponseEntity<FAGroupDTO> create(@Valid @RequestBody FAGroupDTO dto) {
        FAGroup entity = FAGroupDTOMapper.toEntity(dto);
        FAGroup created = createUseCase.execute(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(FAGroupDTOMapper.toDTO(created));
    }

    @PutMapping("/{code}")
    public ResponseEntity<FAGroupDTO> update(@PathVariable String code, @Valid @RequestBody FAGroupDTO dto) {
        FAGroup entity = FAGroupDTOMapper.toEntity(dto);
        entity.setAccountCode(code);
        FAGroup updatedEntity = updateUseCase.execute(entity);
        return ResponseEntity.ok(FAGroupDTOMapper.toDTO(updatedEntity));
    }

    @GetMapping
    public ResponseEntity<List<FAGroupDTO>> getAll() {
        List<FAGroup> entities = getAllUseCase.execute();
        List<FAGroupDTO> dtos = new ArrayList<>();

        for (FAGroup entity : entities) {
            FAGroupDTO dto = FAGroupDTOMapper.toDTO(entity);
            dtos.add(dto);
        }
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{code}/subgroups")
    public ResponseEntity<FAGroupWithSubGroupsDTO> getGroupWithSubGroups(@PathVariable String code) {
        FAGroupWithSubGroupsDTO result = getFAGroupWithSubGroups.execute(code);
        return ResponseEntity.ok(result);
    }

}
