package com.bank.model;

/**
 * Enumeration representing all supported financial transaction types
 * within the Retail Banking Core System.
 */
public enum TransactionType {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER_OUT("Transfer Out"),
    TRANSFER_IN("Transfer In"),
    INTEREST("Interest Credit"),
    FEE("Service Fee");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
