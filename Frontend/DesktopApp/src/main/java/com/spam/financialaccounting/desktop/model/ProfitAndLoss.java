package com.spam.financialaccounting.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfitAndLoss {
    private String asOfDate;
    private List<ReportLineItem> revenue;
    private List<ReportLineItem> expenses;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;

    public ProfitAndLoss() {
    }

    public String getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(String asOfDate) {
        this.asOfDate = asOfDate;
    }

    public List<ReportLineItem> getRevenue() {
        return revenue;
    }

    public void setRevenue(List<ReportLineItem> revenue) {
        this.revenue = revenue;
    }

    public List<ReportLineItem> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<ReportLineItem> expenses) {
        this.expenses = expenses;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }
}
