package com.spam.financialaccounting.presentation.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class FAGroupDTO {

    @NotBlank(message = "Account Code is required")
    @Size(max = 10, message = "Account Code must be at most 10 characters.")
    private String accountCode;  // Account group code (e.g., '01' for Asset)

    @NotBlank(message = "Account Description is required.")
    @Size(max = 100 , message = "Account Description must be at most 100 characters")
    private String accountDescription;  // Account group description (e.g., 'Asset')

    @NotBlank(message = "Account type is required.")
    @Pattern(regexp = "[01]", message = "Account type must be '0' for Asset, '1' for Liability.")
    private String accountType;  // Type (e.g., '0' for Asset, '1' for Liability)
    
    @NotNull(message = "Account current balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must be greater than or equal to 0.")
    private BigDecimal accountCurrentBalance;  // Current balance (for aggregations)

    public FAGroupDTO() {
    }

    //Constructor
    public FAGroupDTO(String accountCode, String accountDescription, String accountType, BigDecimal accountCurrentBalance) {
        this.accountCode = accountCode;
        this.accountDescription = accountDescription;
        this.accountType = accountType;
        this.accountCurrentBalance = accountCurrentBalance;
    }

    //Getters and Setters
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
