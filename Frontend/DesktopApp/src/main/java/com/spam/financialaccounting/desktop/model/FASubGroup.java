package com.spam.financialaccounting.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FASubGroup {
    private String sCode;
    private String sDesc;
    private String aCode;
    private String sType;
    private BigDecimal sOpbal;
    private String sDrCr;
    private String sFlag;

    public FASubGroup() {
    }

    public FASubGroup(String sCode, String sDesc, String aCode, String sType, BigDecimal sOpbal, String sDrCr,
            String sFlag) {
        this.sCode = sCode;
        this.sDesc = sDesc;
        this.aCode = aCode;
        this.sType = sType;
        this.sOpbal = sOpbal;
        this.sDrCr = sDrCr;
        this.sFlag = sFlag;
    }

    @JsonProperty("sCode")
    public String getSCode() {
        return sCode;
    }

    @JsonProperty("sCode")
    public void setSCode(String sCode) {
        this.sCode = sCode;
    }

    @JsonProperty("sDesc")
    public String getSDesc() {
        return sDesc;
    }

    @JsonProperty("sDesc")
    public void setSDesc(String sDesc) {
        this.sDesc = sDesc;
    }

    @JsonProperty("aCode")
    public String getACode() {
        return aCode;
    }

    @JsonProperty("aCode")
    public void setACode(String aCode) {
        this.aCode = aCode;
    }

    @JsonProperty("sType")
    public String getSType() {
        return sType;
    }

    @JsonProperty("sType")
    public void setSType(String sType) {
        this.sType = sType;
    }

    @JsonProperty("sOpbal")
    public BigDecimal getSOpbal() {
        return sOpbal;
    }

    @JsonProperty("sOpbal")
    public void setSOpbal(BigDecimal sOpbal) {
        this.sOpbal = sOpbal;
    }

    @JsonProperty("sDrCr")
    public String getSDrCr() {
        return sDrCr;
    }

    @JsonProperty("sDrCr")
    public void setSDrCr(String sDrCr) {
        this.sDrCr = sDrCr;
    }

    @JsonProperty("sFlag")
    public String getSFlag() {
        return sFlag;
    }

    @JsonProperty("sFlag")
    public void setSFlag(String sFlag) {
        this.sFlag = sFlag;
    }

    @Override
    public String toString() {
        return sCode + " - " + sDesc;
    }
}