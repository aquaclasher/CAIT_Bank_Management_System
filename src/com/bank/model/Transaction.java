package com.bank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable transaction record representing an audited line item in a bank passbook.
 *
 * @param transactionId Unique transaction identifier (UUID)
 * @param timestamp     Exact date and time when the transaction occurred
 * @param type          Type of transaction (DEPOSIT, WITHDRAWAL, etc.)
 * @param amount        Monetary value involved in the transaction
 * @param balanceAfter  Account balance immediately following this transaction
 * @param description   Human-readable memo or audit description
 */
public record Transaction(
        String transactionId,
        LocalDateTime timestamp,
        TransactionType type,
        double amount,
        double balanceAfter,
        String description
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Compact constructor validating non-null mandatory fields.
     */
    public Transaction {
        Objects.requireNonNull(transactionId, "transactionId cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
    }

    /**
     * Convenience constructor generating automatic UUID and current timestamp.
     *
     * @param type         Type of transaction
     * @param amount       Monetary transaction amount
     * @param balanceAfter Resulting account balance
     * @param description  Memo or note
     */
    public Transaction(TransactionType type, double amount, double balanceAfter, String description) {
        this(UUID.randomUUID().toString(), LocalDateTime.now(), type, amount, balanceAfter, description);
    }

    /**
     * Returns a formatted passbook row.
     * Example:
     * [TX-e837194f] 2026-09-16 21:00:00 | DEPOSIT        | Amount: ₹  5,000.00 | Balance: ₹ 15,000.00 | Note: Salary Credit
     */
    @Override
    public String toString() {
        String shortId = transactionId.length() > 8 ? transactionId.substring(0, 8) : transactionId;
        String formattedAmount = String.format("₹%,10.2f", amount);
        String formattedBalance = String.format("₹%,10.2f", balanceAfter);
        return String.format("[TX-%-8s] %s | %-13s | Amount: %s | Balance: %s | Note: %s",
                shortId,
                timestamp.format(FORMATTER),
                type.name(),
                formattedAmount,
                formattedBalance,
                description);
    }
}
