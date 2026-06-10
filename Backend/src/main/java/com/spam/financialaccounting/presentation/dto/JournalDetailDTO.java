package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class JournalDetailDTO {

    @NotBlank(message = "Journal ID is required")
    @Pattern(regexp = "^.{10}$", message = "Journal ID must be exactly 10 characters")
    private String jId;

    @NotBlank(message = "Account code is required")
    @Pattern(regexp = "^.{5}$", message = "Account code must be exactly 5 characters")
    private String jCode;

    @NotBlank(message = "Debit/Credit indicator is required")
    @Pattern(regexp = "^(?i)(DR|CR)$", message = "Debit/Credit indicator must be either 'DR' or 'CR'")
    private String jDrCr;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.001", message = "Amount must be greater than zero")
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