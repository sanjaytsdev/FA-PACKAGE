package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Deleting an individual journal line is disabled: removing one line would leave
 * its voucher unbalanced. Corrections go through ReverseJournalVoucher
 * (POST /api/v1/journal-vouchers/{jId}/reverse).
 */
public class DeleteJournalDetailTest {

    private final DeleteJournalDetail deleteJournalDetail = new DeleteJournalDetail();

    @Test
    @DisplayName("Deleting a single journal line is blocked to keep its voucher balanced")
    void shouldBlockSingleLineDeletion() {
        assertThatThrownBy(() -> deleteJournalDetail.execute("V001", "SG01", "DR"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("disabled")
                .hasMessageContaining("Reverse");
    }
}
