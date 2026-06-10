package com.spam.financialaccounting.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A closed accounting period. As long as a lock covers a date, you can't post
 * (or reverse) a voucher into that date.
 */
public class PeriodLock {
    private Long id;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDateTime lockedAt;
    private String lockedBy;

    public PeriodLock(Long id, LocalDate periodStart, LocalDate periodEnd, LocalDateTime lockedAt, String lockedBy) {
        this.id = id;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }
}
