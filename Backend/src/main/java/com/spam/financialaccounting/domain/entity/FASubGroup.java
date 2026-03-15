package com.spam.financialaccounting.domain.entity;

import java.math.BigDecimal;

public class FASubGroup {
    private String sCode;  // Ledger account code (e.g., '10001' for a specific asset)
    private String sDesc;  // Ledger account description (e.g., 'Cash')
    private String aCode;  // Account group code (foreign key to FAGroup)
    private String sType;  // Type (e.g., '00' for assets, '01' for liabilities)
    private BigDecimal sOpbal;  // Opening balance
    private String sDrCr;  // Normal balance side: 'DR' or 'CR'
    private String sFlag;  // Active flag: 'T' for active, 'F' for inactive

    // Constructor
    public FASubGroup(String sCode, String sDesc, String aCode, String sType, BigDecimal sOpbal, String sDrCr, String sFlag) {
        this.sCode = sCode;
        this.sDesc = sDesc;
        this.aCode = aCode;
        this.sType = sType;
        this.sOpbal = sOpbal;
        this.sDrCr = sDrCr;
        this.sFlag = sFlag;
    }

    // Getters and setters
    public String getSCode() {
        return sCode;
    }

    public void setSCode(String sCode) {
        this.sCode = sCode;
    }

    public String getSDesc() {
        return sDesc;
    }

    public void setSDesc(String sDesc) {
        this.sDesc = sDesc;
    }

    public String getACode() {
        return aCode;
    }

    public void setACode(String aCode) {
        this.aCode = aCode;
    }

    public String getSType() {
        return sType;
    }

    public void setSType(String sType) {
        this.sType = sType;
    }

    public BigDecimal getSOpbal() {
        return sOpbal;
    }

    public void setSOpbal(BigDecimal sOpbal) {
        this.sOpbal = sOpbal;
    }

    public String getSDrCr() {
        return sDrCr;
    }

    public void setSDrCr(String sDrCr) {
        this.sDrCr = sDrCr;
    }

    public String getSFlag() {
        return sFlag;
    }

    public void setSFlag(String sFlag) {
        this.sFlag = sFlag;
    }
}
