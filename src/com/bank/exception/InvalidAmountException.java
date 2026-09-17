package com.bank.exception;

/**
 * Checked exception thrown when an invalid monetary amount (e.g., negative or zero) is supplied.
 */
public class InvalidAmountException extends BankingException {
    private final double invalidAmount;

    public InvalidAmountException(String message, double invalidAmount) {
        super(String.format("%s (Provided amount: $%.2f)", message, invalidAmount));
        this.invalidAmount = invalidAmount;
    }

    public double getInvalidAmount() {
        return invalidAmount;
    }
}
