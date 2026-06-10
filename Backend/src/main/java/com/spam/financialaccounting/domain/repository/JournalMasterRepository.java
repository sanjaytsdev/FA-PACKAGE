package com.spam.financialaccounting.domain.repository;

import java.util.List;
import java.util.Optional;

import com.spam.financialaccounting.domain.entity.JournalMaster;

public interface JournalMasterRepository {
    JournalMaster save(JournalMaster journalMaster);

    Optional<JournalMaster> findById(String jId);

    List<JournalMaster> findAll();

    JournalMaster update(JournalMaster journalMaster);

    boolean delete(String jId);

    boolean existsById(String jId);

    /** True if there's at least one voucher with this document type (e.g. 'OB'). */
    boolean existsByDoc(String jDoc);

    String generateNextId();

    /** J_ID of the voucher that reversed this one, or null if it hasn't been reversed. */
    String getReversedBy(String jId);

    /** J_ID this voucher reverses, or null if it isn't a reversal. */
    String getReverses(String jId);

    /**
     * Links a reversal to its original: sets REVERSED_BY on the original and
     * REVERSES on the reversal voucher.
     */
    void linkReversal(String originalId, String reversalId);
}
