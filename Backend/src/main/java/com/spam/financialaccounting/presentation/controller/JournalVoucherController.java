package com.spam.financialaccounting.presentation.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.journalvoucher.DeleteJournalVoucher;
import com.spam.financialaccounting.application.usecases.journalvoucher.PostJournalVoucher;
import com.spam.financialaccounting.application.usecases.journalvoucher.ReverseJournalVoucher;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalMasterDTOMapper;
import com.spam.financialaccounting.presentation.dto.JournalMasterDTO;
import com.spam.financialaccounting.presentation.dto.JournalVoucherRequestDTO;

import jakarta.validation.Valid;

/**
 * The one place to post and delete whole, balanced journal vouchers. Use this
 * instead of the lower-level /journal-masters and /journal-details endpoints,
 * which can't guarantee a balanced, atomic voucher.
 */
@RestController
@RequestMapping("/api/v1/journal-vouchers")
public class JournalVoucherController {

    private final PostJournalVoucher postUseCase;
    private final DeleteJournalVoucher deleteUseCase;
    private final ReverseJournalVoucher reverseUseCase;

    public JournalVoucherController(PostJournalVoucher postUseCase, DeleteJournalVoucher deleteUseCase,
            ReverseJournalVoucher reverseUseCase) {
        this.postUseCase = postUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reverseUseCase = reverseUseCase;
    }

    @PostMapping
    public ResponseEntity<JournalMasterDTO> post(@Valid @RequestBody JournalVoucherRequestDTO request) {
        JournalMaster header = new JournalMaster(null, request.getJDoc(), request.getJDate(), null, request.getJNarr());
        List<JournalDetail> lines = request.getLines().stream()
                .map(l -> new JournalDetail(null, l.getJCode(), l.getJDrCr(), l.getJAmount()))
                .collect(Collectors.toList());

        JournalMaster created = postUseCase.execute(header, lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalMasterDTOMapper.toDTO(created));
    }

    @PostMapping("/{jId}/reverse")
    public ResponseEntity<JournalMasterDTO> reverse(@PathVariable String jId) {
        JournalMaster reversal = reverseUseCase.execute(jId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalMasterDTOMapper.toDTO(reversal));
    }

    @DeleteMapping("/{jId}")
    public ResponseEntity<Void> delete(@PathVariable String jId) {
        deleteUseCase.execute(jId);
        return ResponseEntity.noContent().build();
    }
}
