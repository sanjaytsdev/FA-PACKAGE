package com.spam.financialaccounting.application.usecases.openingbalance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spam.financialaccounting.domain.entity.DrCr;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.application.usecases.journalvoucher.PostJournalVoucher;
import com.spam.financialaccounting.presentation.exception.openingbalance.OpeningBalanceAlreadyInitializedException;
import com.spam.financialaccounting.presentation.exception.openingbalance.OpeningBalanceImbalanceException;

/**
 * Imports opening balances that set the books' starting state, as one Opening
 * Balance Journal Voucher (J_DOC = 'OB').
 *
 * <p>It's the single entry point for opening balances and it checks the rule the
 * trial balance relies on: total opening debits must equal total opening credits.
 * An unbalanced set is rejected and nothing is saved, so the books can't start
 * out of balance.
 *
 * <p>The posting itself goes through {@link PostJournalVoucher}, which checks the
 * lines point at existing, active accounts, re-checks the debit/credit balance,
 * derives the voucher total, and writes the header and lines in one transaction
 * with an audit record. Since the opening balances are posted as normal journal
 * lines, the trial balance picks them up the usual way — the old
 * {@code FASubGroup.S_OPBAL} field isn't the source of truth anymore.
 */
@Service
public class ImportOpeningBalances {

    /** Document type that marks the opening-balance voucher. */
    public static final String OPENING_BALANCE_DOC = "OB";

    private final PostJournalVoucher postJournalVoucher;
    private final JournalMasterRepository journalMasterRepository;

    public ImportOpeningBalances(PostJournalVoucher postJournalVoucher,
            JournalMasterRepository journalMasterRepository) {
        this.postJournalVoucher = postJournalVoucher;
        this.journalMasterRepository = journalMasterRepository;
    }

    @Transactional
    public JournalMaster execute(List<OpeningBalanceEntry> entries, LocalDateTime openingDate, String narration) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("At least one opening balance entry is required");
        }

        // Opening balances go in once. A second import would just pile duplicate
        // balances on top of the first.
        if (journalMasterRepository.existsByDoc(OPENING_BALANCE_DOC)) {
            throw new OpeningBalanceAlreadyInitializedException(
                    "Opening balances have already been imported; the books are initialized only once. "
                            + "Reverse the existing opening-balance voucher before importing a new set.");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (OpeningBalanceEntry entry : entries) {
            BigDecimal amount = entry.amount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Opening balance amount for account " + entry.accountCode() + " must be greater than zero");
            }
            String side = DrCr.normalizeOrNull(entry.drCr());
            if (side == null) {
                throw new IllegalArgumentException(
                        "Opening balance side for account " + entry.accountCode() + " must be 'DR' or 'CR'");
            }
            if (DrCr.DEBIT.equals(side)) {
                totalDebits = totalDebits.add(amount);
            } else {
                totalCredits = totalCredits.add(amount);
            }
        }

        // Debits must equal credits, or the books start out of balance.
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new OpeningBalanceImbalanceException(totalDebits, totalCredits);
        }

        JournalMaster header = new JournalMaster(null, OPENING_BALANCE_DOC, openingDate, null,
                narration != null && !narration.isBlank() ? narration : "Opening Balance");
        List<JournalDetail> lines = entries.stream()
                .map(e -> new JournalDetail(null, e.accountCode(), DrCr.normalizeOrNull(e.drCr()), e.amount()))
                .collect(Collectors.toList());

        return postJournalVoucher.execute(header, lines);
    }
}
