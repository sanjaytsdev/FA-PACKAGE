package com.spam.financialaccounting.desktop.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JournalMaster {
    private String jId;
    private String jDoc;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime jDate;
    private BigDecimal jAmount;
    private String jNarr;

    public JournalMaster() {
    }

    public JournalMaster(String jId, String jDoc, LocalDateTime jDate, BigDecimal jAmount, String jNarr) {
        this.jId = jId;
        this.jDoc = jDoc;
        this.jDate = jDate;
        this.jAmount = jAmount;
        this.jNarr = jNarr;
    }

    @JsonProperty("jId")
    public String getJId() {
        return jId;
    }

    @JsonProperty("jId")
    public void setJId(String jId) {
        this.jId = jId;
    }

    @JsonProperty("jDoc")
    public String getJDoc() {
        return jDoc;
    }

    @JsonProperty("jDoc")
    public void setJDoc(String jDoc) {
        this.jDoc = jDoc;
    }

    @JsonProperty("jDate")
    public LocalDateTime getJDate() {
        return jDate;
    }

    @JsonProperty("jDate")
    public void setJDate(LocalDateTime jDate) {
        this.jDate = jDate;
    }

    @JsonProperty("jAmount")
    public BigDecimal getJAmount() {
        return jAmount;
    }

    @JsonProperty("jAmount")
    public void setJAmount(BigDecimal jAmount) {
        this.jAmount = jAmount;
    }

    @JsonProperty("jNarr")
    public String getJNarr() {
        return jNarr;
    }

    @JsonProperty("jNarr")
    public void setJNarr(String jNarr) {
        this.jNarr = jNarr;
    }
}