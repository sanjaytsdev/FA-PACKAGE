package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TrialBalanceDTO {

    private final LocalDate asOfDate;
    private final List<Row> rows;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final boolean balanced;

    public TrialBalanceDTO(LocalDate asOfDate, List<Row> rows, BigDecimal totalDebit, BigDecimal totalCredit,
            boolean balanced) {
        this.asOfDate = asOfDate;
        this.rows = rows;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.balanced = balanced;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public List<Row> getRows() {
        return rows;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public static class Row {
        private final String accountCode;
        private final String description;
        private final BigDecimal debit;
        private final BigDecimal credit;

        public Row(String accountCode, String description, BigDecimal debit, BigDecimal credit) {
            this.accountCode = accountCode;
            this.description = description;
            this.debit = debit;
            this.credit = credit;
        }

        public String getAccountCode() {
            return accountCode;
        }

        public String getDescription() {
            return description;
        }

        public BigDecimal getDebit() {
            return debit;
        }

        public BigDecimal getCredit() {
            return credit;
        }
    }
}
