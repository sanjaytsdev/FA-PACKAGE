package com.spam.financialaccounting.domain.repository;

import java.math.BigDecimal;

/**
 * Posted debit/credit totals for one ledger account, summed by the database
 * instead of in memory. Comes from {@link JournalDetailRepository#sumPostingsAsOf}.
 */
public record AccountPostingTotals(String accountCode, BigDecimal totalDebit, BigDecimal totalCredit) {
}
