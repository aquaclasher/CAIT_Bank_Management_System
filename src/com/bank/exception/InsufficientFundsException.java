package com.bank.exception;

/**
 * Checked exception thrown when a withdrawal or debit violates account minimum balance rules.
 */
public class InsufficientFundsException extends BankingException {
    private final double currentBalance;
    private final double requestedAmount;
    private final double minimumBalance;

    public InsufficientFundsException(double currentBalance, double requestedAmount, double minimumBalance) {
        super(String.format(
                "Insufficient funds: Current balance is ₹%.2f, requested withdrawal is ₹%.2f, but minimum required balance is ₹%.2f (Maximum withdrawable: ₹%.2f).",
                currentBalance, requestedAmount, minimumBalance, Math.max(0.0, currentBalance - minimumBalance)
        ));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.minimumBalance = minimumBalance;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    public double getMinimumBalance() {
        return minimumBalance;
    }
}
