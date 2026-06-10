package com.spam.financialaccounting.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceSheet {
    private String asOfDate;
    private List<ReportLineItem> assets;
    private List<ReportLineItem> liabilities;
    private List<ReportLineItem> equity;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private boolean balanced;

    public BalanceSheet() {
    }

    public String getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(String asOfDate) {
        this.asOfDate = asOfDate;
    }

    public List<ReportLineItem> getAssets() {
        return assets;
    }

    public void setAssets(List<ReportLineItem> assets) {
        this.assets = assets;
    }

    public List<ReportLineItem> getLiabilities() {
        return liabilities;
    }

    public void setLiabilities(List<ReportLineItem> liabilities) {
        this.liabilities = liabilities;
    }

    public List<ReportLineItem> getEquity() {
        return equity;
    }

    public void setEquity(List<ReportLineItem> equity) {
        this.equity = equity;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }

    public void setTotalLiabilities(BigDecimal totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }

    public BigDecimal getTotalEquity() {
        return totalEquity;
    }

    public void setTotalEquity(BigDecimal totalEquity) {
        this.totalEquity = totalEquity;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }
}
