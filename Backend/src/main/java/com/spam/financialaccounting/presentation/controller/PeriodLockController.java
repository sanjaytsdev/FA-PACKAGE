package com.spam.financialaccounting.presentation.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.periodlock.CreatePeriodLock;
import com.spam.financialaccounting.domain.entity.PeriodLock;
import com.spam.financialaccounting.infrastructure.persistence.repository.PeriodLockRepository;
import com.spam.financialaccounting.presentation.dto.PeriodLockRequestDTO;

import jakarta.validation.Valid;

/**
 * Manages closed accounting periods. Once a period is locked, postings and
 * reversals dated inside it get rejected.
 */
@RestController
@RequestMapping("/api/v1/period-locks")
public class PeriodLockController {

    private final PeriodLockRepository periodLockRepository;
    private final CreatePeriodLock createPeriodLock;

    public PeriodLockController(PeriodLockRepository periodLockRepository, CreatePeriodLock createPeriodLock) {
        this.periodLockRepository = periodLockRepository;
        this.createPeriodLock = createPeriodLock;
    }

    @PostMapping
    public ResponseEntity<PeriodLock> create(@Valid @RequestBody PeriodLockRequestDTO request) {
        // TODO: use the logged-in user here once auth is in place
        PeriodLock lock = new PeriodLock(null, request.getPeriodStart(), request.getPeriodEnd(),
                LocalDateTime.now(), "SYSTEM");
        PeriodLock created = createPeriodLock.execute(lock);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PeriodLock>> getAll() {
        return ResponseEntity.ok(periodLockRepository.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        periodLockRepository.delete(id);
        return ResponseEntity.noContent().build();
    }
}
