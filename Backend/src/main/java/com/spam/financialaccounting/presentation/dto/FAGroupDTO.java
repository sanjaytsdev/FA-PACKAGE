package com.spam.financialaccounting.presentation.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class FAGroupDTO {

    @NotBlank(message = "Account Code is mandatory")
    @Size(min = 2, max = 2, message = "Account Code must be exactly 2 characters")
    private String accountCode; // Account group code (e.g., '01' for Asset)

    @NotBlank(message = "Description is mandatory")
    @Size(max = 50, message = "Description cannot exceed 50 characters")
    private String accountDescription; // Account group description (e.g., 'Asset')

    @NotBlank(message = "Account Type is mandatory")
    @Pattern(regexp = "^[0-4]$", message = "Type must be 0, 1, 2, 3, or 4")
    private String accountType; // Type (e.g., '0' for Asset, '1' for Liability)

    private BigDecimal accountCurrentBalance; // Current balance (for aggregations)

    public FAGroupDTO() {
    }

    // Constructor
    public FAGroupDTO(String accountCode, String accountDescription, String accountType,
            BigDecimal accountCurrentBalance) {
        this.accountCode = accountCode;
        this.accountDescription = accountDescription;
        this.accountType = accountType;
        this.accountCurrentBalance = accountCurrentBalance;
    }

    // Getters and Setters
    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountDescription() {
        return accountDescription;
    }

    public void setAccountDescription(String accountDescription) {
        this.accountDescription = accountDescription;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getAccountCurrentBalance() {
        return accountCurrentBalance;
    }

    public void setAccountCurrentBalance(BigDecimal accountCurrentBalance) {
        this.accountCurrentBalance = accountCurrentBalance;
    }
}
