package com.spam.financialaccounting.application.usecases.journalmaster;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.entity.JournalMaster;

/**
 * Creating a journal-voucher header directly is disabled.
 *
 * A header without its balanced lines isn't a valid double-entry voucher and
 * shouldn't be saved on its own. All posting goes through
 * {@link com.spam.financialaccounting.application.usecases.journalvoucher.PostJournalVoucher}
 * (POST /api/v1/journal-vouchers), which keeps the entry balanced and atomic.
 * This bean is just a fail-fast guard: if something wires it up, the call fails
 * right away instead of writing bad data.
 */
@Service
public class CreateJournalMaster {

    public JournalMaster execute(JournalMaster journalMaster) {
        throw new UnsupportedOperationException(
                "Direct journal-master creation is disabled; post a complete, balanced "
                        + "voucher via PostJournalVoucher (POST /api/v1/journal-vouchers).");
    }
}
