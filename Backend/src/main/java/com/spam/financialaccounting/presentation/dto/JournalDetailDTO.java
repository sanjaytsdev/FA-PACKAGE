package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class JournalDetailDTO {

    @NotBlank(message = "Journal ID is required")
    private String jId;

    @NotBlank(message = "Account code is required")
    private String jCode;

    @NotBlank(message = "Debit/Credit indicator is required")
    private String jDrCr;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal jAmount;

    public JournalDetailDTO() {
    }

    public JournalDetailDTO(String jId, String jCode, String jDrCr, BigDecimal jAmount) {
        this.jId = jId;
        this.jCode = jCode;
        this.jDrCr = jDrCr;
        this.jAmount = jAmount;
    }

    @JsonProperty("jId")
    public String getJId() {
        return jId;
    }

    @JsonProperty("jCode")
    public String getJCode() {
        return jCode;
    }

    @JsonProperty("jDrCr")
    public String getJDrCr() {
        return jDrCr;
    }

    @JsonProperty("jAmount")
    public BigDecimal getJAmount() {
        return jAmount;
    }

    @JsonProperty("jId")
    public void setJId(String jId) {
        this.jId = jId;
    }

    @JsonProperty("jCode")
    public void setJCode(String jCode) {
        this.jCode = jCode;
    }

    @JsonProperty("jDrCr")
    public void setJDrCr(String jDrCr) {
        this.jDrCr = jDrCr;
    }

    @JsonProperty("jAmount")
    public void setJAmount(BigDecimal jAmount) {
        this.jAmount = jAmount;
    }
}