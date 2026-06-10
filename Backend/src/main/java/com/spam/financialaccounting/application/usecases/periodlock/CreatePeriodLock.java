package com.spam.financialaccounting.application.usecases.periodlock;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.PeriodLock;
import com.spam.financialaccounting.infrastructure.persistence.repository.PeriodLockRepository;
import com.spam.financialaccounting.presentation.exception.periodlock.PeriodLockOverlapException;
import com.spam.financialaccounting.presentation.exception.periodlock.PeriodLockValidationException;

/**
 * Creates a closed accounting period. The range has to be in order (start before
 * end) and can't overlap a period that's already locked.
 */
@Service
public class CreatePeriodLock {

    private final PeriodLockRepository periodLockRepository;

    public CreatePeriodLock(PeriodLockRepository periodLockRepository) {
        this.periodLockRepository = periodLockRepository;
    }

    public PeriodLock execute(PeriodLock lock) {
        LocalDate start = lock.getPeriodStart();
        LocalDate end = lock.getPeriodEnd();

        if (start.isAfter(end)) {
            throw new PeriodLockValidationException(
                    "Period start (" + start + ") must not be after period end (" + end + ")");
        }

        for (PeriodLock existing : periodLockRepository.findAll()) {
            // Report an exact match separately from a partial overlap.
            if (start.equals(existing.getPeriodStart()) && end.equals(existing.getPeriodEnd())) {
                throw new PeriodLockOverlapException(
                        "A lock for the period " + start + " to " + end + " already exists");
            }
            // Two inclusive ranges overlap when each starts on or before the other ends.
            if (!start.isAfter(existing.getPeriodEnd()) && !existing.getPeriodStart().isAfter(end)) {
                throw new PeriodLockOverlapException(
                        "Period " + start + " to " + end + " overlaps an existing lock ("
                                + existing.getPeriodStart() + " to " + existing.getPeriodEnd() + ")");
            }
        }

        return periodLockRepository.save(lock);
    }
}
