package com.bank.exception;

/**
 * Checked exception thrown when attempting to register an account with a duplicate number.
 */
public class DuplicateAccountException extends BankingException {
    private final String accountNumber;

    public DuplicateAccountException(String accountNumber) {
        super("Bank account already exists with account number: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
