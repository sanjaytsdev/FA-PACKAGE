package com.spam.financialaccounting.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.spam.financialaccounting.domain.entity.JournalDetail;

public interface JournalDetailRepository {
    void save(JournalDetail detail);
    Optional<JournalDetail> findByCompositeKey(String jId, String jCode, String jDrCr);
    List<JournalDetail> findByJournalId(String jId);
    List<JournalDetail> findByAccountCode(String jCode);
    List<JournalDetail> findAll();

    /**
     * Sums posted debits and credits per account for vouchers dated on or before
     * {@code asOfDate}. The DB does the aggregation and returns one row per account.
     * Future-dated vouchers are left out so historical reports stay correct.
     */
    List<AccountPostingTotals> sumPostingsAsOf(LocalDate asOfDate);
    JournalDetail update(JournalDetail detail);
    boolean deleteByCompositeKey(String jId, String jCode, String jDrCr);
    boolean deleteByJournalId(String jId);
    boolean existsByCompositeKey(String jId, String jCode, String jDrCr);
    boolean existsByAccountCode(String jCode);
}
