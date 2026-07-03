package com.spam.financialaccounting.application.usecases.journalvoucher;

import org.springframework.stereotype.Service;

import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.PostedVoucherCannotBeDeletedException;

/**
 * Hard-deleting a posted voucher would make it disappear from the ledger, which
 * is bad for audits. Use {@link ReverseJournalVoucher} to correct things instead.
 *
 * TODO: there's no draft/unposted status yet. Once there is, allow deleting only
 * drafts here and keep blocking posted vouchers. For now every voucher in the
 * ledger is posted, so all deletes are blocked.
 */
@Service
public class DeleteJournalVoucher {

    private final JournalMasterRepository journalMasterRepository;

    public DeleteJournalVoucher(JournalMasterRepository journalMasterRepository) {
        this.journalMasterRepository = journalMasterRepository;
    }

    public void execute(String jId) {
        if (!journalMasterRepository.existsById(jId)) {
            throw new JournalMasterNotFoundException("Journal voucher with ID " + jId + " not found");
        }

        throw new PostedVoucherCannotBeDeletedException(
                "Journal voucher " + jId + " is posted and cannot be deleted; reverse it instead");
    }
}
