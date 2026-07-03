package com.spam.financialaccounting.application.usecases.journaldetail;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalDetail;

/**
 * Creating a single journal line directly is disabled.
 *
 * Saving one line on its own gives you a single-sided, unbalanced entry, which
 * breaks double-entry. All posting goes through
 * {@link com.spam.financialaccounting.application.usecases.journalvoucher.PostJournalVoucher}
 * (POST /api/v1/journal-vouchers), which validates and saves a whole balanced
 * voucher in one transaction. This bean is just a fail-fast guard.
 */
@Service
public class CreateJournalDetail {

    public JournalDetail execute(JournalDetail detail) {
        throw new UnsupportedOperationException(
                "Direct journal-detail creation is disabled; a single line is a single-sided, "
                        + "unbalanced entry. Post a complete, balanced voucher via PostJournalVoucher "
                        + "(POST /api/v1/journal-vouchers).");
    }
}
