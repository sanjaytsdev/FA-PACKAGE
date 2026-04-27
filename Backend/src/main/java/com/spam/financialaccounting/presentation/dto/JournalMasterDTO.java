package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class JournalMasterDTO {

    @NotBlank(message = "Journal ID is required")
    @Pattern(regexp = "^.{10}$", message = "Journal ID must be exactly 10 characters")
    private String jId;

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "^.{2}$", message = "Document type must be exactly 2 characters")
    private String jDoc;

    @NotNull(message = "Journal date is required")
    private LocalDateTime jDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", message = "Amount must be zero or positive")
    private BigDecimal jAmount;

    @Size(max = 100, message = "Narration must not exceed 100 characters")
    private String jNarr;

    public JournalMasterDTO() {
    }

    public JournalMasterDTO(String jId, String jDoc, LocalDateTime jDate, BigDecimal jAmount, String jNarr) {
        this.jId = jId;
        this.jDoc = jDoc;
        this.jDate = jDate;
        this.jAmount = jAmount;
        this.jNarr = jNarr;
    }

    // getters
    @JsonProperty("jId")
    public String getJId() {
        return jId;
    }

    @JsonProperty("jDoc")
    public String getJDoc() {
        return jDoc;
    }

    @JsonProperty("jDate")
    public LocalDateTime getJDate() {
        return jDate;
    }

    @JsonProperty("jAmount")
    public BigDecimal getJAmount() {
        return jAmount;
    }

    @JsonProperty("jNarr")
    public String getJNarr() {
        return jNarr;
    }

    // setters
    @JsonProperty("jId")
    public void setJId(String jId) {
        this.jId = jId;
    }

    @JsonProperty("jDoc")
    public void setJDoc(String jDoc) {
        this.jDoc = jDoc;
    }

    @JsonProperty("jDate")
    public void setJDate(LocalDateTime jDate) {
        this.jDate = jDate;
    }

    @JsonProperty("jAmount")
    public void setJAmount(BigDecimal jAmount) {
        this.jAmount = jAmount;
    }

    @JsonProperty("jNarr")
    public void setJNarr(String jNarr) {
        this.jNarr = jNarr;
    }
}
