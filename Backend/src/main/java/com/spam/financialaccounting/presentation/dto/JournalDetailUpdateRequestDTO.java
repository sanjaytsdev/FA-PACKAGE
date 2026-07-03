package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class JournalDetailUpdateRequestDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.001", message = "Amount must be greater than zero")
    private BigDecimal jAmount;

    public JournalDetailUpdateRequestDTO() {
    }

    public JournalDetailUpdateRequestDTO(BigDecimal jAmount) {
        this.jAmount = jAmount;
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
