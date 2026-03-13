package com.spam.financialaccounting.domain.entity;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class JournalLine {
    private Long id;
    private LedgerAccount account;
    private BigDecimal debit;
    private BigDecimal credit;
}
