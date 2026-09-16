package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;

/**
 * Savings Account implementation enforcing a strict minimum balance requirement
 * and supporting periodic interest credit calculation.
 */
public class SavingsAccount extends BankAccount {
    public static final double MINIMUM_BALANCE = 1000.00;
    private double interestRate;

    /**
     * Constructs a new SavingsAccount instance.
     *
     * @param accountNumber  Unique account number
     * @param owner          Customer owning this account
     * @param initialDeposit Initial opening deposit (must be >= 0)
     * @param interestRate   Annual/periodic interest rate (e.g., 0.04 for 4.0%)
     * @throws InvalidAmountException if initialDeposit is negative or interestRate is negative
     */
    public SavingsAccount(String accountNumber, Customer owner, double initialDeposit, double interestRate)
            throws InvalidAmountException {
        super(accountNumber, owner, initialDeposit);
        if (interestRate < 0.0) {
            throw new InvalidAmountException("Interest rate cannot be negative", interestRate);
        }
        this.interestRate = interestRate;
    }

    @Override
    public String getAccountType() {
        return "Savings Account";
    }

    public static double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }

    public double getInterestRate() {
        synchronized (getLock()) {
            return interestRate;
        }
    }

    public void setInterestRate(double interestRate) throws InvalidAmountException {
        if (interestRate < 0.0) {
            throw new InvalidAmountException("Interest rate cannot be negative", interestRate);
        }
        synchronized (getLock()) {
            this.interestRate = interestRate;
        }
    }

    /**
     * Withdraws funds from the savings account enforcing the minimum balance invariant.
     *
     * @param amount Amount to withdraw
     * @throws InvalidAmountException     if amount <= 0
     * @throws InsufficientFundsException if (balance - amount) < MINIMUM_BALANCE
     */
    @Override
    public void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException {
        withdrawInternal(amount, TransactionType.WITHDRAWAL, "Cash / ATM Withdrawal");
    }

    /**
     * Withdraws funds for inter-account transfer enforcing the minimum balance invariant.
     *
     * @param amount      Amount to transfer out
     * @param description Custom transfer description
     * @throws InvalidAmountException     if amount <= 0
     * @throws InsufficientFundsException if (balance - amount) < MINIMUM_BALANCE
     */
    @Override
    public void withdrawTransfer(double amount, String description)
            throws InvalidAmountException, InsufficientFundsException {
        withdrawInternal(amount, TransactionType.TRANSFER_OUT, description);
    }

    /**
     * Atomic withdrawal logic under fine-grained lock.
     */
    private void withdrawInternal(double amount, TransactionType type, String description)
            throws InvalidAmountException, InsufficientFundsException {
        if (amount <= 0.0) {
            throw new InvalidAmountException("Withdrawal amount must be strictly positive", amount);
        }

        synchronized (getLock()) {
            if ((balance - amount) < MINIMUM_BALANCE) {
                throw new InsufficientFundsException(balance, amount, MINIMUM_BALANCE);
            }
            balance -= amount;
            Transaction tx = new Transaction(type, amount, balance, description);
            addTransaction(tx);
        }
    }

    /**
     * Calculates interest on current balance and credits it via an INTEREST transaction.
     *
     * @return The amount of interest credited
     */
    public double applyInterest() {
        synchronized (getLock()) {
            if (balance <= 0.0) {
                return 0.0;
            }
            double interest = balance * interestRate;
            if (interest > 0.0) {
                balance += interest;
                String desc = String.format("Periodic Interest Credit @ %.2f%%", interestRate * 100.0);
                Transaction tx = new Transaction(TransactionType.INTEREST, interest, balance, desc);
                addTransaction(tx);
            }
            return interest;
        }
    }
}
