package com.bank.service;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.OverdraftLimitExceededException;

/**
 * Contract for core monetary transaction operations on bank accounts.
 * Demonstrates method overloading and invariant validation.
 */
public interface Transactable {

    /**
     * Deposits a positive monetary amount into the account with a default description.
     *
     * @param amount Monetary amount to deposit (must be strictly positive)
     * @throws InvalidAmountException if amount is less than or equal to zero
     */
    void deposit(double amount) throws InvalidAmountException;

    /**
     * Overloaded deposit method accepting a custom transaction note/memo.
     *
     * @param amount Monetary amount to deposit (must be strictly positive)
     * @param note   Custom description or memo for the transaction
     * @throws InvalidAmountException if amount is less than or equal to zero
     */
    void deposit(double amount, String note) throws InvalidAmountException;

    /**
     * Withdraws a positive monetary amount from the account according to account-specific rules.
     *
     * @param amount Monetary amount to withdraw (must be strictly positive)
     * @throws InvalidAmountException          if amount is less than or equal to zero
     * @throws InsufficientFundsException      if withdrawal violates minimum balance rules
     * @throws OverdraftLimitExceededException if withdrawal exceeds sanctioned overdraft limits
     */
    void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException, OverdraftLimitExceededException;
}
