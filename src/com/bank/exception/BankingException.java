package com.bank.exception;

/**
 * Base checked exception for all banking domain errors and invariant violations.
 */
public class BankingException extends Exception {

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
