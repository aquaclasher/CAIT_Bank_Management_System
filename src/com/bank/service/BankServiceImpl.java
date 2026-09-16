package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankingException;
import com.bank.exception.CustomerNotFoundException;
import com.bank.exception.InvalidAmountException;
import com.bank.model.BankAccount;
import com.bank.model.CurrentAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe production implementation of the {@link BankService}.
 * Enforces transaction atomicity, deterministic deadlock prevention on multi-account transfers,
 * and thread-safe collection management.
 */
public class BankServiceImpl implements BankService {
    private final Map<String, BankAccount> accounts = new ConcurrentHashMap<>();
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();

    private final AtomicInteger customerSequence = new AtomicInteger(1000);
    private final AtomicInteger savingsSequence = new AtomicInteger(100000);
    private final AtomicInteger currentSequence = new AtomicInteger(500000);

    /**
     * Default constructor.
     */
    public BankServiceImpl() {
    }

    @Override
    public Customer registerCustomer(String name, String email, String phone) throws BankingException {
        if (name == null || name.trim().isEmpty()) {
            throw new BankingException("Customer name cannot be null or empty.");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new BankingException("A valid customer email address is required.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new BankingException("Customer phone number cannot be null or empty.");
        }

        String customerId = "CUST-" + customerSequence.incrementAndGet();
        Customer customer = new Customer(customerId, name.trim(), email.trim(), phone.trim());
        customers.put(customerId, customer);
        return customer;
    }

    @Override
    public BankAccount openSavingsAccount(String customerId, double initialDeposit, double interestRate)
            throws BankingException {
        Customer customer = findCustomer(customerId);
        if (initialDeposit < 0.0) {
            throw new InvalidAmountException("Initial deposit cannot be negative", initialDeposit);
        }
        if (interestRate < 0.0) {
            throw new InvalidAmountException("Interest rate cannot be negative", interestRate);
        }

        String accNum = "SAV-" + savingsSequence.incrementAndGet();
        SavingsAccount account = new SavingsAccount(accNum, customer, initialDeposit, interestRate);
        accounts.put(accNum, account);
        return account;
    }

    @Override
    public BankAccount openCurrentAccount(String customerId, double initialDeposit, double overdraftLimit)
            throws BankingException {
        Customer customer = findCustomer(customerId);
        if (initialDeposit < 0.0) {
            throw new InvalidAmountException("Initial deposit cannot be negative", initialDeposit);
        }
        if (overdraftLimit < 0.0) {
            throw new InvalidAmountException("Overdraft limit cannot be negative", overdraftLimit);
        }

        String accNum = "CUR-" + currentSequence.incrementAndGet();
        CurrentAccount account = new CurrentAccount(accNum, customer, initialDeposit, overdraftLimit);
        accounts.put(accNum, account);
        return account;
    }

    @Override
    public BankAccount findAccount(String accountNumber) throws AccountNotFoundException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new AccountNotFoundException("Provided account number is null or empty");
        }
        BankAccount account = accounts.get(accountNumber.trim().toUpperCase());
        if (account == null) {
            // Also attempt case-sensitive match
            account = accounts.get(accountNumber.trim());
        }
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    @Override
    public Customer findCustomer(String customerId) throws CustomerNotFoundException {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new CustomerNotFoundException("Provided customer ID is null or empty");
        }
        Customer customer = customers.get(customerId.trim().toUpperCase());
        if (customer == null) {
            customer = customers.get(customerId.trim());
        }
        if (customer == null) {
            throw new CustomerNotFoundException(customerId);
        }
        return customer;
    }

    @Override
    public void deposit(String accountNumber, double amount) throws BankingException {
        BankAccount account = findAccount(accountNumber);
        account.deposit(amount);
    }

    @Override
    public void deposit(String accountNumber, double amount, String note) throws BankingException {
        BankAccount account = findAccount(accountNumber);
        account.deposit(amount, note);
    }

    @Override
    public void withdraw(String accountNumber, double amount) throws BankingException {
        BankAccount account = findAccount(accountNumber);
        account.withdraw(amount);
    }

    /**
     * Executes a thread-safe, deadlock-free inter-account transfer.
     *
     * Invariant & Deadlock Prevention:
     * When transferring between two accounts (A and B), concurrent opposite transfers (B to A)
     * could produce a classic dining philosophers deadlock if locks are acquired arbitrarily.
     * To guarantee deadlock freedom, the locks on both accounts are acquired in a strict,
     * globally consistent order based on their lexicographical account number comparison.
     */
    @Override
    public void transfer(String fromAccNum, String toAccNum, double amount, String description) throws BankingException {
        if (fromAccNum == null || toAccNum == null) {
            throw new BankingException("Account numbers for transfer cannot be null.");
        }
        if (fromAccNum.trim().equalsIgnoreCase(toAccNum.trim())) {
            throw new InvalidAmountException("Cannot initiate transfer to the same account (" + fromAccNum + ")", amount);
        }
        if (amount <= 0.0) {
            throw new InvalidAmountException("Transfer amount must be strictly positive", amount);
        }

        BankAccount fromAccount = findAccount(fromAccNum);
        BankAccount toAccount = findAccount(toAccNum);

        // Deterministic Lock Ordering: Sort by account number comparison
        BankAccount firstLockAccount;
        BankAccount secondLockAccount;

        if (fromAccount.getAccountNumber().compareTo(toAccount.getAccountNumber()) < 0) {
            firstLockAccount = fromAccount;
            secondLockAccount = toAccount;
        } else {
            firstLockAccount = toAccount;
            secondLockAccount = fromAccount;
        }

        String narrative = (description == null || description.isBlank())
                ? String.format("Transfer to %s", toAccount.getAccountNumber())
                : description;
        String senderNarrative = String.format("Transfer to %s - %s", toAccount.getAccountNumber(), narrative);
        String receiverNarrative = String.format("Transfer from %s - %s", fromAccount.getAccountNumber(), narrative);

        // Acquire locks in deterministic sequence
        synchronized (firstLockAccount.getLock()) {
            synchronized (secondLockAccount.getLock()) {
                // Perform atomic withdraw & deposit
                fromAccount.withdrawTransfer(amount, senderNarrative);
                toAccount.depositTransfer(amount, receiverNarrative);
            }
        }
    }

    @Override
    public List<BankAccount> getAllAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accounts.values()));
    }

    @Override
    public List<Customer> getAllCustomers() {
        return Collections.unmodifiableList(new ArrayList<>(customers.values()));
    }

    @Override
    public List<BankAccount> getAccountsForCustomer(String customerId) throws CustomerNotFoundException {
        Customer customer = findCustomer(customerId);
        List<String> accountNums = customer.getAssociatedAccountNumbers();
        List<BankAccount> result = new ArrayList<>();
        for (String accNum : accountNums) {
            BankAccount acc = accounts.get(accNum);
            if (acc != null) {
                result.add(acc);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void applyInterestToSavingsAccounts() {
        for (BankAccount account : accounts.values()) {
            if (account instanceof SavingsAccount savingsAccount) {
                savingsAccount.applyInterest();
            }
        }
    }
}
