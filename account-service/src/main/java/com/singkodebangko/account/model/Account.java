package com.singkodebangko.account.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Mutable Entity representing a Bank Account aligned to Singko de Bangko contracts.
 * Implemented as a standard POJO without Lombok.
 */
public class Account {

    private Long accountId;
    private String accountNumber;
    private Long userId;
    private String accountType;
    private String ownerName;
    private String email;
    private BigDecimal balance;
    private String currency;
    private String status;
    private Instant updatedAt;

    public Account() {
    }

    public Account(Long accountId, String accountNumber, Long userId, String accountType,
                   String ownerName, String email, BigDecimal balance, String currency,
                   String status, Instant updatedAt) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.userId = userId;
        this.accountType = accountType != null ? accountType : "SAVINGS";
        this.ownerName = ownerName;
        this.email = email;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.currency = currency != null ? currency : "PHP";
        this.status = status != null ? status : "ACTIVE";
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(accountId, account.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", accountNumber='" + accountNumber + '\'' +
                ", userId=" + userId +
                ", accountType='" + accountType + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", email='" + email + '\'' +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
