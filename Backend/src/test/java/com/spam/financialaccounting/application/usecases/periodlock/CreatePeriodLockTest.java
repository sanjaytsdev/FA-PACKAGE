package com.spam.financialaccounting.application.usecases.periodlock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.PeriodLock;
import com.spam.financialaccounting.infrastructure.persistence.repository.PeriodLockRepository;
import com.spam.financialaccounting.presentation.exception.periodlock.PeriodLockOverlapException;
import com.spam.financialaccounting.presentation.exception.periodlock.PeriodLockValidationException;

@ExtendWith(MockitoExtension.class)
public class CreatePeriodLockTest {

    @Mock
    private PeriodLockRepository periodLockRepository;

    @InjectMocks
    private CreatePeriodLock createPeriodLock;

    private PeriodLock lock(String start, String end) {
        return new PeriodLock(null, LocalDate.parse(start), LocalDate.parse(end),
                LocalDateTime.now(), "SYSTEM");
    }

    @Test
    @DisplayName("Should save when the range is well-ordered and collides with nothing")
    void shouldSave_WhenValid() {
        when(periodLockRepository.findAll()).thenReturn(List.of());
        PeriodLock toSave = lock("2026-01-01", "2026-01-31");
        when(periodLockRepository.save(any(PeriodLock.class))).thenReturn(toSave);

        PeriodLock result = createPeriodLock.execute(toSave);

        assertThat(result).isSameAs(toSave);
        verify(periodLockRepository).save(toSave);
    }

    @Test
    @DisplayName("Should reject a period whose start is after its end")
    void shouldThrow_WhenStartAfterEnd() {
        assertThatThrownBy(() -> createPeriodLock.execute(lock("2026-01-31", "2026-01-01")))
                .isInstanceOf(PeriodLockValidationException.class)
                .hasMessageContaining("must not be after");

        verify(periodLockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject a period that overlaps an existing lock")
    void shouldThrow_WhenOverlapping() {
        when(periodLockRepository.findAll())
                .thenReturn(List.of(lock("2026-01-01", "2026-01-31")));

        // New period starts inside the existing locked range.
        assertThatThrownBy(() -> createPeriodLock.execute(lock("2026-01-15", "2026-02-15")))
                .isInstanceOf(PeriodLockOverlapException.class)
                .hasMessageContaining("overlaps");

        verify(periodLockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject a duplicate of an existing lock range")
    void shouldThrow_WhenDuplicate() {
        when(periodLockRepository.findAll())
                .thenReturn(List.of(lock("2026-01-01", "2026-01-31")));

        assertThatThrownBy(() -> createPeriodLock.execute(lock("2026-01-01", "2026-01-31")))
                .isInstanceOf(PeriodLockOverlapException.class)
                .hasMessageContaining("already exists");

        verify(periodLockRepository, never()).save(any());
    }
}
