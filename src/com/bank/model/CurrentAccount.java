package com.bank.model;

import com.bank.exception.InvalidAmountException;
import com.bank.exception.OverdraftLimitExceededException;

/**
 * Current (checking) account implementation supporting an approved overdraft credit limit
 * and automatic overdraft penalty fee assessment.
 */
public class CurrentAccount extends BankAccount {
    public static final double OVERDRAFT_FEE = 50.00;
    private double overdraftLimit;

    /**
     * Constructs a new CurrentAccount instance.
     *
     * @param accountNumber  Unique account number
     * @param owner          Customer owning this account
     * @param initialDeposit Initial opening deposit (must be >= 0)
     * @param overdraftLimit Sanctioned credit line/overdraft limit (must be >= 0)
     * @throws InvalidAmountException if initialDeposit or overdraftLimit is negative
     */
    public CurrentAccount(String accountNumber, Customer owner, double initialDeposit, double overdraftLimit)
            throws InvalidAmountException {
        super(accountNumber, owner, initialDeposit);
        if (overdraftLimit < 0.0) {
            throw new InvalidAmountException("Overdraft limit cannot be negative", overdraftLimit);
        }
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public String getAccountType() {
        return "Current Account";
    }

    public static double getOverdraftFee() {
        return OVERDRAFT_FEE;
    }

    public double getOverdraftLimit() {
        synchronized (getLock()) {
            return overdraftLimit;
        }
    }

    public void setOverdraftLimit(double overdraftLimit) throws InvalidAmountException {
        if (overdraftLimit < 0.0) {
            throw new InvalidAmountException("Overdraft limit cannot be negative", overdraftLimit);
        }
        synchronized (getLock()) {
            this.overdraftLimit = overdraftLimit;
        }
    }

    /**
     * Withdraws funds from the current account up to the allowed balance + overdraft limit.
     * If the withdrawal causes balance to go below zero (utilizing overdraft),
     * an OVERDRAFT_FEE is assessed and recorded.
     *
     * @param amount Amount to withdraw
     * @throws InvalidAmountException          if amount <= 0
     * @throws OverdraftLimitExceededException if amount > (balance + overdraftLimit)
     */
    @Override
    public void withdraw(double amount) throws InvalidAmountException, OverdraftLimitExceededException {
        withdrawInternal(amount, TransactionType.WITHDRAWAL, "Current Account Debit / Withdrawal");
    }

    /**
     * Withdraws funds for inter-account transfer enforcing overdraft limits.
     *
     * @param amount      Amount to transfer out
     * @param description Custom transfer narrative
     * @throws InvalidAmountException          if amount <= 0
     * @throws OverdraftLimitExceededException if amount > (balance + overdraftLimit)
     */
    @Override
    public void withdrawTransfer(double amount, String description)
            throws InvalidAmountException, OverdraftLimitExceededException {
        withdrawInternal(amount, TransactionType.TRANSFER_OUT, description);
    }

    /**
     * Atomic withdrawal and fee calculation under fine-grained lock.
     */
    private void withdrawInternal(double amount, TransactionType type, String description)
            throws InvalidAmountException, OverdraftLimitExceededException {
        if (amount <= 0.0) {
            throw new InvalidAmountException("Withdrawal amount must be strictly positive", amount);
        }

        synchronized (getLock()) {
            double totalAvailableFunds = balance + overdraftLimit;
            if (amount > totalAvailableFunds) {
                throw new OverdraftLimitExceededException(balance, amount, overdraftLimit);
            }

            boolean overdraftTriggered = (amount > balance);
            balance -= amount;
            Transaction tx = new Transaction(type, amount, balance, description);
            addTransaction(tx);

            if (overdraftTriggered) {
                balance -= OVERDRAFT_FEE;
                Transaction feeTx = new Transaction(
                        TransactionType.FEE,
                        OVERDRAFT_FEE,
                        balance,
                        String.format("Overdraft Facility Utilization Fee (₹%.2f assessed)", OVERDRAFT_FEE)
                );
                addTransaction(feeTx);
            }
        }
    }
}
