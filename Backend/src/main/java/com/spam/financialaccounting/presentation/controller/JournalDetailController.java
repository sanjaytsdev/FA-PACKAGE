package com.spam.financialaccounting.presentation.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.journaldetail.GetAllJournalDetails;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetail;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetailsByAccountCode;
import com.spam.financialaccounting.application.usecases.journaldetail.GetJournalDetailsByJournalId;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalDetailDTOMapper;
import com.spam.financialaccounting.presentation.dto.JournalDetailDTO;

/**
 * Read-only access to journal voucher lines.
 *
 * No create/update/delete here, on purpose. Editing one line on its own would
 * break the double-entry balance of its voucher, so all posting and removal goes
 * through {@link JournalVoucherController} (/api/v1/journal-vouchers), which
 * validates and saves a whole balanced voucher in one shot.
 */
@RestController
@RequestMapping("/api/v1/journal-details")
public class JournalDetailController {

    private final GetJournalDetail getUseCase;
    private final GetAllJournalDetails getAllUseCase;
    private final GetJournalDetailsByJournalId getByJournalIdUseCase;
    private final GetJournalDetailsByAccountCode getByAccountCodeUseCase;

    public JournalDetailController(GetJournalDetail getUseCase,
            GetAllJournalDetails getAllUseCase,
            GetJournalDetailsByJournalId getByJournalIdUseCase,
            GetJournalDetailsByAccountCode getByAccountCodeUseCase) {
        this.getUseCase = getUseCase;
        this.getAllUseCase = getAllUseCase;
        this.getByJournalIdUseCase = getByJournalIdUseCase;
        this.getByAccountCodeUseCase = getByAccountCodeUseCase;
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

}
