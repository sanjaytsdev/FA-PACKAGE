package com.spam.financialaccounting.application.usecases.openingbalance;

import java.math.BigDecimal;

/**
 * One opening-balance line: the ledger account, which side it sits on ('DR' or
 * 'CR'), and the positive amount.
 */
public record OpeningBalanceEntry(String accountCode, String drCr, BigDecimal amount) {
}
