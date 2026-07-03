package com.spam.financialaccounting.application.usecases.journalmaster;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.spam.financialaccounting.domain.entity.JournalMaster;

/**
 * Direct journal-master creation is disabled to guarantee double-entry integrity:
 * a header without its balanced lines must never be persisted. All posting goes
 * through PostJournalVoucher (POST /api/v1/journal-vouchers).
 */
public class CreateJournalMasterTest {

    private final CreateJournalMaster createJournalMaster = new CreateJournalMaster();

    @Test
    @DisplayName("Direct journal-master creation is blocked and routes callers to PostJournalVoucher")
    void shouldBlockDirectCreation() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", LocalDateTime.now(),
                new BigDecimal("1000.00"), "Test entry");

        assertThatThrownBy(() -> createJournalMaster.execute(master))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("disabled")
                .hasMessageContaining("PostJournalVoucher");
    }
}
