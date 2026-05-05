package com.spam.financialaccounting.presentation.controller;

import com.spam.financialaccounting.application.usecases.journalmaster.UpdateJournalMaster;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.journalmaster.CreateJournalMaster;
import com.spam.financialaccounting.application.usecases.journalmaster.DeleteJournalMaster;
import com.spam.financialaccounting.application.usecases.journalmaster.GetAllJournalMaster;
import com.spam.financialaccounting.application.usecases.journalmaster.GetJournalMasterById;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalMasterDTOMapper;
import com.spam.financialaccounting.presentation.dto.JournalMasterDTO;

import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.catalina.connector.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;




@RestController
@RequestMapping("/api/v1/journal-masters")
public class JournalMasterController {
    
    private final CreateJournalMaster createUseCase;
    private final UpdateJournalMaster updateUseCase;
    private final DeleteJournalMaster deleteUseCase;
    private final GetJournalMasterById getByIdUseCase;
    private final GetAllJournalMaster getAllUseCase;
    
    public JournalMasterController(
        CreateJournalMaster createUseCase,
        UpdateJournalMaster updateUseCase,
        DeleteJournalMaster deleteUseCase,
        GetJournalMasterById getByIdUseCase,
        GetAllJournalMaster getAllUseCase

    ){
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getByIdUseCase = getByIdUseCase;
        this.getAllUseCase = getAllUseCase;
    }

    @PostMapping
    public ResponseEntity<JournalMasterDTO> create(@Valid @RequestBody JournalMasterDTO dto){
        JournalMaster entity = JournalMasterDTOMapper.toEntity(dto);
        JournalMaster created = createUseCase.execute(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalMasterDTOMapper.toDTO(created));
    }

    @PutMapping("/{jId}")
    public ResponseEntity<JournalMasterDTO> update(
        @PathVariable String jId,
        @Valid @RequestBody JournalMasterDTO dto
    ){
        dto.setJId(jId);
        JournalMaster entity = JournalMasterDTOMapper.toEntity(dto);
        JournalMaster updated = updateUseCase.execute(entity);
        return ResponseEntity.ok(JournalMasterDTOMapper.toDTO(updated));
    }

    @DeleteMapping("/{jId}")
    public ResponseEntity<Void> delete(@PathVariable String jId) {
        deleteUseCase.execute(jId);
        return ResponseEntity.noContent().build();
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
