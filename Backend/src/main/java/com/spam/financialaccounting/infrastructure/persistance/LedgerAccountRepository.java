package com.spam.financialaccounting.infrastructure.persistance;

import com.spam.financialaccounting.domain.entity.LedgerAccount;

public interface LedgerAccountRepository {
    void save(LedgerAccount ledgerAccount);
    LedgerAccount findById(Long id);
}
