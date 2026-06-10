package com.spam.financialaccounting.presentation.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.openingbalance.ImportOpeningBalances;
import com.spam.financialaccounting.application.usecases.openingbalance.OpeningBalanceEntry;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalMasterDTOMapper;
import com.spam.financialaccounting.presentation.dto.JournalMasterDTO;
import com.spam.financialaccounting.presentation.dto.OpeningBalanceImportDTO;

import jakarta.validation.Valid;

/**
 * Sets up the books with their opening balances.
 *
 * <p>The import only goes through when total opening debits equal total opening
 * credits. If they don't, it's rejected and nothing is saved. On success the
 * balances are written as a single Opening Balance Journal Voucher, returned
 * here as the created header.
 */
@RestController
@RequestMapping("/api/v1/opening-balances")
public class OpeningBalanceController {

    private final ImportOpeningBalances importOpeningBalances;

    public OpeningBalanceController(ImportOpeningBalances importOpeningBalances) {
        this.importOpeningBalances = importOpeningBalances;
    }

    @PostMapping("/import")
    public ResponseEntity<JournalMasterDTO> importOpeningBalances(
            @Valid @RequestBody OpeningBalanceImportDTO request) {
        List<OpeningBalanceEntry> entries = request.getLines().stream()
                .map(l -> new OpeningBalanceEntry(l.getAccountCode(), l.getDrCr(), l.getAmount()))
                .collect(Collectors.toList());

        LocalDateTime openingDate = request.getOpeningDate() != null
                ? request.getOpeningDate().atStartOfDay()
                : null;

        JournalMaster voucher = importOpeningBalances.execute(entries, openingDate, request.getNarration());
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalMasterDTOMapper.toDTO(voucher));
    }
}
