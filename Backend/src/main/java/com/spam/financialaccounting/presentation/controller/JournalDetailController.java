package com.spam.financialaccounting.presentation.controller;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.journaldetail.CreateJournalDetail;
import com.spam.financialaccounting.application.usecases.journaldetail.DeleteJournalDetail;
import com.spam.financialaccounting.application.usecases.journaldetail.GetAllJournalDetails;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetail;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetailsByAccountCode;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetailsByJournalId;
import com.spam.financialaccounting.application.usecases.journaldetail.UpdateJournalDetail;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalDetailDTOMapper;
import com.spam.financialaccounting.presentation.dto.JournalDetailDTO;
import com.spam.financialaccounting.presentation.dto.JournalDetailUpdateRequestDTO;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/journal-details")
public class JournalDetailController {

    private final CreateJournalDetail createUseCase;
    private final GetJournalDetail getUseCase;
    private final GetAllJournalDetails getAllUseCase;
    private final GetJournalDetailsByJournalId getByJournalIdUseCase;
    private final GetJournalDetailsByAccountCode getByAccountCodeUseCase;
    private final UpdateJournalDetail updateUseCase;
    private final DeleteJournalDetail deleteUseCase;

    public JournalDetailController(CreateJournalDetail createUseCase,
            GetJournalDetail getUseCase,
            GetAllJournalDetails getAllUseCase,
            GetJournalDetailsByJournalId getByJournalIdUseCase,
            GetJournalDetailsByAccountCode getByAccountCodeUseCase,
            UpdateJournalDetail updateUseCase,
            DeleteJournalDetail deleteUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.getAllUseCase = getAllUseCase;
        this.getByJournalIdUseCase = getByJournalIdUseCase;
        this.getByAccountCodeUseCase = getByAccountCodeUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @GetMapping("/{jId}/{jCode}/{jDrCr}")
    public ResponseEntity<JournalDetailDTO> get(@PathVariable String jId,
            @PathVariable String jCode,
            @PathVariable String jDrCr) {
        JournalDetail detail = getUseCase.execute(jId, jCode, jDrCr);
        return ResponseEntity.ok(JournalDetailDTOMapper.toDTO(detail));
    }

    @GetMapping
    public ResponseEntity<List<JournalDetailDTO>> getAll() {
        List<JournalDetail> details = getAllUseCase.execute();
        List<JournalDetailDTO> dtos = details.stream().map(JournalDetailDTOMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/journal/{jId}")
    public ResponseEntity<List<JournalDetailDTO>> getByJournalId(@PathVariable String jId) {
        List<JournalDetail> details = getByJournalIdUseCase.execute(jId);
        List<JournalDetailDTO> dtos = details.stream().map(JournalDetailDTOMapper::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/account/{jCode}")
    public ResponseEntity<List<JournalDetailDTO>> getByAccountCode(@PathVariable String jCode) {
        List<JournalDetail> details = getByAccountCodeUseCase.execute(jCode);
        List<JournalDetailDTO> dtos = details.stream()
                .map(JournalDetailDTOMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<JournalDetailDTO> create(@Valid @RequestBody JournalDetailDTO dto) {
        JournalDetail entity = JournalDetailDTOMapper.toEntity(dto);
        JournalDetail created = createUseCase.execute(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalDetailDTOMapper.toDTO(created));
    }

    @PutMapping("/{jId}/{jCode}/{jDrCr}")
    public ResponseEntity<JournalDetailDTO> update(@PathVariable String jId,
            @PathVariable String jCode,
            @PathVariable String jDrCr,
            @Valid @RequestBody JournalDetailUpdateRequestDTO request) {
        JournalDetail updated = updateUseCase.execute(jId, jCode, jDrCr, request.getJAmount());
        return ResponseEntity.ok(JournalDetailDTOMapper.toDTO(updated));
    }

    @DeleteMapping("/{jId}/{jCode}/{jDrCr}")
    public ResponseEntity<Void> delete(
            @PathVariable String jId,
            @PathVariable String jCode,
            @PathVariable String jDrCr) {
        deleteUseCase.execute(jId, jCode, jDrCr);
        return ResponseEntity.noContent().build();

    }

}
