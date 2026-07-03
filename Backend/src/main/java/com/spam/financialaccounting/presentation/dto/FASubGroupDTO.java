
package com.spam.financialaccounting.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class FASubGroupDTO {

    @NotBlank(message = "Ledger Code is mandatory")
    @Size(min = 5, max = 5, message = "Ledger Code must be exactly 5 characters")
    private String sCode;

    @NotBlank(message = "Description is mandatory")
    @Size(max = 50, message = "Description cannot exceed 50 characters")
    private String sDesc;

    @NotBlank(message = "Parent Group Code is mandatory")
    @Size(min = 2, max = 2, message = "Parent Group Code must be exactly 2 characters")
    private String aCode;

    @NotBlank(message = "Type is mandatory")
    @Size(min = 2, max = 2, message = "Type must be exactly 2 characters")
    @Pattern(regexp = "^[0-4][0-9]$", message = "Type must be a valid 2-digit account type (00-49)")
    private String sType;

    @DecimalMin(value = "0.0", message = "Opening balance must be positive or zero")
    @Digits(integer = 15, fraction = 3, message = "Amount must have max 15 integer and 3 decimal places")
    private BigDecimal sOpbal;

    @Pattern(regexp = "^(?i)(DR|CR)$", message = "Must be DR or CR")
    private String sDrCr;

    @Pattern(regexp = "^(?i)(T|F)$", message = "Must be T (active) or F (inactive)")
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
    @JsonProperty("sCode")
    public String getSCode() {
        return sCode;
    }

    @JsonProperty("sDesc")
    public String getSDesc() {
        return sDesc;
    }

    @JsonProperty("aCode")
    public String getACode() {
        return aCode;
    }

    @JsonProperty("sType")
    public String getSType() {
        return sType;
    }

    @JsonProperty("sOpbal")
    public BigDecimal getSOpbal() {
        return sOpbal;
    }

    @JsonProperty("sDrCr")
    public String getSDrCr() {
        return sDrCr;
    }

    @JsonProperty("sFlag")
    public String getSFlag() {
        return sFlag;
    }

    // --- SETTERS ---

    @JsonProperty("sCode")
    public void setSCode(String sCode) {
        this.sCode = sCode;
    }

    @JsonProperty("sDesc")  
    public void setSDesc(String sDesc) {
        this.sDesc = sDesc;
    }

    @JsonProperty("aCode")
    public void setACode(String aCode) {
        this.aCode = aCode;
    }

    @JsonProperty("sType")
    public void setSType(String sType) {
        this.sType = sType;
    }

    @JsonProperty("sOpbal")
    public void setSOpbal(BigDecimal sOpbal) {
        this.sOpbal = sOpbal;
    }

    @JsonProperty("sDrCr")
    public void setSDrCr(String sDrCr) {
        this.sDrCr = sDrCr;
    }

    @JsonProperty("sFlag")
    public void setSFlag(String sFlag) {
        this.sFlag = sFlag;
    }
}