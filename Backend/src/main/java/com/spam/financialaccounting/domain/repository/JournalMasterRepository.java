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
}
