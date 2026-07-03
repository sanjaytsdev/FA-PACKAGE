package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Balance Sheet as of a reporting date. Assets, liabilities and equity each show
 * on their natural side. Unclosed earnings (the P&amp;L net result so far) go in
 * the equity section as a "Net Income" line, so the accounting equation holds:
 * {@code totalAssets == totalLiabilities + totalEquity}.
 */
public class BalanceSheetDTO {

    private final LocalDate asOfDate;
    private final List<LineItem> assets;
    private final List<LineItem> liabilities;
    private final List<LineItem> equity;
    private final BigDecimal totalAssets;
    private final BigDecimal totalLiabilities;
    private final BigDecimal totalEquity;
    private final boolean balanced;

    public BalanceSheetDTO(LocalDate asOfDate, List<LineItem> assets, List<LineItem> liabilities,
            List<LineItem> equity, BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal totalEquity,
            boolean balanced) {
        this.asOfDate = asOfDate;
        this.assets = assets;
        this.liabilities = liabilities;
        this.equity = equity;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.totalEquity = totalEquity;
        this.balanced = balanced;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public List<LineItem> getAssets() {
        return assets;
    }

    public List<LineItem> getLiabilities() {
        return liabilities;
    }

    public List<LineItem> getEquity() {
        return equity;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }

    public BigDecimal getTotalEquity() {
        return totalEquity;
    }

    public boolean isBalanced() {
        return balanced;
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
