package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Profit &amp; Loss (income statement) as of a reporting date: revenue and
 * expense accounts on their natural sides, plus the net result.
 */
public class ProfitAndLossDTO {

    private final LocalDate asOfDate;
    private final List<LineItem> revenue;
    private final List<LineItem> expenses;
    private final BigDecimal totalRevenue;
    private final BigDecimal totalExpenses;
    private final BigDecimal netProfit;

    public ProfitAndLossDTO(LocalDate asOfDate, List<LineItem> revenue, List<LineItem> expenses,
            BigDecimal totalRevenue, BigDecimal totalExpenses, BigDecimal netProfit) {
        this.asOfDate = asOfDate;
        this.revenue = revenue;
        this.expenses = expenses;
        this.totalRevenue = totalRevenue;
        this.totalExpenses = totalExpenses;
        this.netProfit = netProfit;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public List<LineItem> getRevenue() {
        return revenue;
    }

    public List<LineItem> getExpenses() {
        return expenses;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public static class LineItem {
        private final String accountCode;
        private final String description;
        private final BigDecimal amount;

        public LineItem(String accountCode, String description, BigDecimal amount) {
            this.accountCode = accountCode;
            this.description = description;
            this.amount = amount;
        }

        public String getAccountCode() {
            return accountCode;
        }

        public String getDescription() {
            return description;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
