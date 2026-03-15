package com.spam.financialaccounting.domain.entity;

import java.math.BigDecimal;

public class JournalDetail {
    private String jId;  // Journal voucher ID (foreign key to JournalMaster)
    private String jCode;  // Account code (foreign key to FASubGroup)
    private String jDrCr;  // Debit or Credit ('DR' or 'CR')
    private BigDecimal jAmount;  // Amount for this line

    // Constructor
    public JournalDetail(String jId, String jCode, String jDrCr, BigDecimal jAmount) {
        this.jId = jId;
        this.jCode = jCode;
        this.jDrCr = jDrCr;
        this.jAmount = jAmount;
    }

    // Getters and setters
    public String getJId() {
        return jId;
    }

    public void setJId(String jId) {
        this.jId = jId;
    }

    public String getJCode() {
        return jCode;
    }

    public void setJCode(String jCode) {
        this.jCode = jCode;
    }

    public String getJDrCr() {
        return jDrCr;
    }

    public void setJDrCr(String jDrCr) {
        this.jDrCr = jDrCr;
    }

    public BigDecimal getJAmount() {
        return jAmount;
    }

    public void setJAmount(BigDecimal jAmount) {
        this.jAmount = jAmount;
    }
}
