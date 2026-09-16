package com.bank.exception;

/**
 * Checked exception thrown when a withdrawal exceeds an account's combined ledger balance and sanctioned overdraft limit.
 */
public class OverdraftLimitExceededException extends BankingException {
    private final double currentBalance;
    private final double requestedAmount;
    private final double overdraftLimit;

    public OverdraftLimitExceededException(double currentBalance, double requestedAmount, double overdraftLimit) {
        super(String.format(
                "Overdraft limit exceeded: Current balance is ₹%.2f, overdraft limit is ₹%.2f (Total credit line: ₹%.2f), but requested withdrawal is ₹%.2f.",
                currentBalance, overdraftLimit, currentBalance + overdraftLimit, requestedAmount
        ));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.overdraftLimit = overdraftLimit;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    public double getOverdraftLimit() {
        return overdraftLimit;
    }
}
