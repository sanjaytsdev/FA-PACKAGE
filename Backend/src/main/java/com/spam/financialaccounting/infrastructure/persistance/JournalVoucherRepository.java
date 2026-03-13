package com.spam.financialaccounting.infrastructure.persistance;

import com.spam.financialaccounting.domain.entity.JournalVoucher;

public interface JournalVoucherRepository {
    void save(JournalVoucher voucher);
    JournalVoucher findById(Long id);
}
