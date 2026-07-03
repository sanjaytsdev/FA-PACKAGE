package com.spam.financialaccounting.application.usecases.journaldetail;

import org.springframework.stereotype.Service;

/**
 * Deleting a single journal line is disabled.
 *
 * Removing one line of a posted voucher leaves the rest unbalanced, which breaks
 * double-entry. Posted vouchers are immutable; correct them through
 * {@link com.spam.financialaccounting.application.usecases.journalvoucher.ReverseJournalVoucher}
 * (POST /api/v1/journal-vouchers/{jId}/reverse), which posts a balanced reversing
 * voucher. This bean is just a fail-fast guard.
 */
@Service
public class DeleteJournalDetail {

    public boolean execute(String jId, String jCode, String jDrCr) {
        throw new UnsupportedOperationException(
                "Deleting an individual journal line is disabled; it would unbalance its voucher. "
                        + "Reverse the whole voucher via ReverseJournalVoucher "
                        + "(POST /api/v1/journal-vouchers/{jId}/reverse) instead.");
    }
}
