package com.bank.exception;

/**
 * Checked exception thrown when an operation references a non-existent customer ID.
 */
public class CustomerNotFoundException extends BankingException {
    private final String customerId;

    public CustomerNotFoundException(String customerId) {
        super("Customer not found with ID: " + customerId);
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }
}
