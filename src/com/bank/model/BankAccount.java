package com.bank.model;

import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.OverdraftLimitExceededException;
import com.bank.service.Reportable;
import com.bank.service.Transactable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class representing a core bank account.
 * Implements {@link Transactable} and {@link Reportable}.
 * Encapsulates thread-safety with fine-grained synchronization locks,
 * defensive copying for audit histories, and object equality invariants.
 */
public abstract class BankAccount implements Transactable, Reportable {
    private final String accountNumber;
    private final Customer owner;
    protected double balance;
    private final List<Transaction> transactions;
    private final Object lock = new Object();

    /**
     * Initializes a bank account with invariant checking on opening balance.
     *
     * @param accountNumber  Unique alphanumeric account number
     * @param owner          The customer owning this account
     * @param initialDeposit Opening deposit amount (must be non-negative)
     * @throws InvalidAmountException if initialDeposit is negative
     */
    public BankAccount(String accountNumber, Customer owner, double initialDeposit) throws InvalidAmountException {
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber cannot be null");
        this.owner = Objects.requireNonNull(owner, "owner cannot be null");
        if (initialDeposit < 0.0) {
            throw new InvalidAmountException("Initial opening deposit cannot be negative", initialDeposit);
        }
        this.transactions = Collections.synchronizedList(new ArrayList<>());
        this.balance = initialDeposit;

        if (initialDeposit > 0.0) {
            Transaction initialTx = new Transaction(
                    TransactionType.DEPOSIT,
                    initialDeposit,
                    this.balance,
                    "Account Opening Initial Deposit"
            );
            this.transactions.add(initialTx);
        }

        // Maintain bi-directional relationship with customer
        this.owner.addAccount(accountNumber);
    }

    /**
     * Fine-grained lock object for concurrency control and deterministic deadlock prevention.
     *
     * @return Dedicated lock instance
     */
    public Object getLock() {
        return lock;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Customer getOwner() {
        return owner;
    }

    /**
     * Returns the current balance in a thread-safe manner.
     *
     * @return Current ledger balance
     */
    public double getBalance() {
        synchronized (lock) {
            return balance;
        }
    }

    /**
     * Returns the concrete account type name for reporting.
     *
     * @return Human-readable account type
     */
    public abstract String getAccountType();

    /**
     * Standard deposit implementation.
     * Rejects amounts <= 0, synchronizes balance updates, and appends a DEPOSIT transaction.
     *
     * @param amount Monetary amount to deposit
     * @throws InvalidAmountException if amount <= 0
     */
    @Override
    public void deposit(double amount) throws InvalidAmountException {
        deposit(amount, "Standard Deposit");
    }

    /**
     * Overloaded deposit implementation accepting a custom memo note.
     * Rejects amounts <= 0, synchronizes balance updates, and appends a DEPOSIT transaction.
     *
     * @param amount Monetary amount to deposit
     * @param note   Custom memo note
     * @throws InvalidAmountException if amount <= 0
     */
    @Override
    public void deposit(double amount, String note) throws InvalidAmountException {
        if (amount <= 0.0) {
            throw new InvalidAmountException("Deposit amount must be strictly greater than zero", amount);
        }
        synchronized (lock) {
            balance += amount;
            Transaction tx = new Transaction(
                    TransactionType.DEPOSIT,
                    amount,
                    balance,
                    (note == null || note.isBlank()) ? "Standard Deposit" : note
            );
            transactions.add(tx);
        }
    }

    /**
     * Internal method to credit incoming transfer funds atomically under lock.
     *
     * @param amount      Monetary amount credited
     * @param description Transfer narrative
     * @throws InvalidAmountException if amount <= 0
     */
    public void depositTransfer(double amount, String description) throws InvalidAmountException {
        if (amount <= 0.0) {
            throw new InvalidAmountException("Transfer deposit amount must be strictly greater than zero", amount);
        }
        synchronized (lock) {
            balance += amount;
            Transaction tx = new Transaction(
                    TransactionType.TRANSFER_IN,
                    amount,
                    balance,
                    description
            );
            transactions.add(tx);
        }
    }

    /**
     * Abstract method enforcing polymorphic account-specific withdrawal logic.
     *
     * @param amount Amount to withdraw
     * @throws InvalidAmountException          if amount <= 0
     * @throws InsufficientFundsException      if violating minimum balance rules
     * @throws OverdraftLimitExceededException if exceeding overdraft limit
     */
    @Override
    public abstract void withdraw(double amount)
            throws InvalidAmountException, InsufficientFundsException, OverdraftLimitExceededException;

    /**
     * Abstract method for polymorphic withdrawal during inter-account transfers.
     *
     * @param amount      Amount to transfer out
     * @param description Narrative description
     * @throws InvalidAmountException          if amount <= 0
     * @throws InsufficientFundsException      if violating minimum balance rules
     * @throws OverdraftLimitExceededException if exceeding overdraft limit
     */
    public abstract void withdrawTransfer(double amount, String description)
            throws InvalidAmountException, InsufficientFundsException, OverdraftLimitExceededException;

    /**
     * Protected helper method for subclasses to record internal audited transactions (e.g., interest, fees).
     *
     * @param tx Transaction record to append
     */
    protected void addTransaction(Transaction tx) {
        synchronized (lock) {
            transactions.add(tx);
        }
    }

    /**
     * Audit Protection: Returns an unmodifiable defensive copy of the transactions list.
     *
     * @return Defensive unmodifiable list of transactions
     */
    @Override
    public List<Transaction> getTransactionHistory() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(transactions));
        }
    }

    /**
     * Generates a fully formatted statement resembling a formal banking passbook ledger.
     *
     * @return Formatted statement string
     */
    @Override
    public String generateStatement() {
        synchronized (lock) {
            StringBuilder sb = new StringBuilder();
            sb.append("========================================================================================================\n");
            sb.append(String.format("                               OFFICIAL BANK STATEMENT - %s\n", getAccountType().toUpperCase()));
            sb.append("========================================================================================================\n");
            sb.append(String.format(" Account Number : %-20s Generated At : %s\n",
                    accountNumber, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            sb.append(String.format(" Primary Holder : %-20s Customer ID  : %s\n",
                    owner.getFullName(), owner.getCustomerId()));
            sb.append(String.format(" Contact Email  : %-20s Phone        : %s\n",
                    owner.getEmail(), owner.getPhone()));
            sb.append(String.format(" Current Balance: $%,.2f\n", balance));
            sb.append("--------------------------------------------------------------------------------------------------------\n");
            sb.append(String.format(" %-12s | %-19s | %-13s | %-14s | %-14s | %s\n",
                    "TX ID", "TIMESTAMP", "TYPE", "AMOUNT", "BALANCE AFTER", "DESCRIPTION"));
            sb.append("--------------------------------------------------------------------------------------------------------\n");

            if (transactions.isEmpty()) {
                sb.append("                                    No transactions recorded yet.\n");
            } else {
                for (Transaction tx : transactions) {
                    String shortId = tx.transactionId().length() > 8 ? tx.transactionId().substring(0, 8) : tx.transactionId();
                    sb.append(String.format(" TX-%-9s | %-19s | %-13s | $%,12.2f | $%,12.2f | %s\n",
                            shortId,
                            tx.timestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            tx.type().name(),
                            tx.amount(),
                            tx.balanceAfter(),
                            tx.description()));
                }
            }
            sb.append("========================================================================================================\n");
            return sb.toString();
        }
    }

    /**
     * Identity Contract: Two accounts are equal if and only if their accountNumber is identical.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankAccount that = (BankAccount) o;
        return Objects.equals(accountNumber, that.accountNumber);
    }

    /**
     * Hash code based strictly on accountNumber.
     */
    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return String.format("%s[AccNum: %s | Owner: %s (%s) | Balance: $%,.2f | TxCount: %d]",
                    getAccountType(), accountNumber, owner.getFullName(), owner.getCustomerId(), balance, transactions.size());
        }
    }
}
