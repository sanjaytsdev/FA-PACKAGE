package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.spam.financialaccounting.domain.entity.JournalDetail;

/**
 * Direct creation of a single journal line is disabled: one line in isolation is
 * a single-sided, unbalanced entry. All posting goes through PostJournalVoucher
 * (POST /api/v1/journal-vouchers), which only accepts whole balanced vouchers.
 */
public class CreateJournalDetailTest {

    private final CreateJournalDetail createJournalDetail = new CreateJournalDetail();

    @Test
    @DisplayName("Direct single-line creation is blocked to prevent single-sided entries")
    void shouldBlockDirectSingleLineCreation() {
        JournalDetail singleSidedLine = new JournalDetail("V001", "SG01", "DR", new BigDecimal("500.00"));

        assertThatThrownBy(() -> createJournalDetail.execute(singleSidedLine))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("disabled")
                .hasMessageContaining("PostJournalVoucher");
    }
}
