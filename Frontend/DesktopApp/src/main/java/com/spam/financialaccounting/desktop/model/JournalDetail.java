package com.spam.financialaccounting.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JournalDetail {
    private String jId;
    private String jCode;
    private String jDrCr;
    private BigDecimal jAmount;

    public JournalDetail() {
    }

    public JournalDetail(String jId, String jCode, String jDrCr, BigDecimal jAmount) {
        this.jId = jId;
        this.jCode = jCode;
        this.jDrCr = jDrCr;
        this.jAmount = jAmount;
    }

    @JsonProperty("jId")
    public String getJId() {
        return jId;
    }

    @JsonProperty("jId")
    public void setJId(String jId) {
        this.jId = jId;
    }

    @JsonProperty("jCode")
    public String getJCode() {
        return jCode;
    }

    @JsonProperty("jCode")
    public void setJCode(String jCode) {
        this.jCode = jCode;
    }

    @JsonProperty("jDrCr")
    public String getJDrCr() {
        return jDrCr;
    }

    @JsonProperty("jDrCr")
    public void setJDrCr(String jDrCr) {
        this.jDrCr = jDrCr;
    }

    @JsonProperty("jAmount")
    public BigDecimal getJAmount() {
        return jAmount;
    }

    @JsonProperty("jAmount")
    public void setJAmount(BigDecimal jAmount) {
        this.jAmount = jAmount;
    }
}