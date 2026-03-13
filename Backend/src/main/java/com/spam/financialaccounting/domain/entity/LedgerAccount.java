package com.spam.financialaccounting.domain.entity;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class LedgerAccount {
    private Long id;
    private String name;
    private AccountGroup accountGroup;
    private BigDecimal balance;
}
