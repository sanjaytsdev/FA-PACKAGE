
package com.spam.financialaccounting.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class FASubGroupDTO {

    @NotBlank(message = "Ledger Code is mandatory")
    private String sCode;

    @NotBlank(message = "Description is mandatory")
    private String sDesc;

    @NotBlank(message = "Parent Group Code is mandatory")
    private String aCode;

    private String sType;
    private BigDecimal sOpbal;
    private String sDrCr;
    private String sFlag;

    // Default Constructor
    public FASubGroupDTO() {
    }

    // All-Args Constructor
    public FASubGroupDTO(String sCode, String sDesc, String aCode, String sType, BigDecimal sOpbal, String sDrCr,
            String sFlag) {
        this.sCode = sCode;
        this.sDesc = sDesc;
        this.aCode = aCode;
        this.sType = sType;
        this.sOpbal = sOpbal;
        this.sDrCr = sDrCr;
        this.sFlag = sFlag;
    }

    // --- GETTERS ---

    public String getSCode() {
        return sCode;
    }

    public String getSDesc() {
        return sDesc;
    }

    public String getACode() {
        return aCode;
    }

    public String getSType() {
        return sType;
    }

    public BigDecimal getSOpbal() {
        return sOpbal;
    }

    public String getSDrCr() {
        return sDrCr;
    }

    public String getSFlag() {
        return sFlag;
    }

    // --- SETTERS ---

    public void setSCode(String sCode) {
        this.sCode = sCode;
    }

    public void setSDesc(String sDesc) {
        this.sDesc = sDesc;
    }

    public void setACode(String aCode) {
        this.aCode = aCode;
    }

    public void setSType(String sType) {
        this.sType = sType;
    }

    public void setSOpbal(BigDecimal sOpbal) {
        this.sOpbal = sOpbal;
    }

    public void setSDrCr(String sDrCr) {
        this.sDrCr = sDrCr;
    }

    public void setSFlag(String sFlag) {
        this.sFlag = sFlag;
    }
}