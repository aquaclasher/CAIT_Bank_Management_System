package com.bank.service;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.BankingException;
import com.bank.exception.CustomerNotFoundException;
import com.bank.model.BankAccount;
import com.bank.model.Customer;

import java.util.List;

/**
 * Service interface defining the administrative and transactional capabilities
 * of the core banking engine.
 */
public interface BankService {

    /**
     * Registers a new customer in the core banking system.
     *
     * @param name  Full customer name
     * @param email Contact email address
     * @param phone Contact phone number
     * @return The registered {@link Customer} object
     * @throws BankingException if input validation fails
     */
    Customer registerCustomer(String name, String email, String phone) throws BankingException;

    /**
     * Opens a new Savings Account for an existing customer.
     *
     * @param customerId   ID of the existing customer
     * @param initialDeposit Initial opening deposit
     * @param interestRate   Periodic interest rate
     * @return The newly created {@link BankAccount}
     * @throws BankingException if customer does not exist or parameters are invalid
     */
    BankAccount openSavingsAccount(String customerId, double initialDeposit, double interestRate) throws BankingException;

    /**
     * Opens a new Current Account for an existing customer.
     *
     * @param customerId     ID of the existing customer
     * @param initialDeposit Initial opening deposit
     * @param overdraftLimit Approved overdraft credit limit
     * @return The newly created {@link BankAccount}
     * @throws BankingException if customer does not exist or parameters are invalid
     */
    BankAccount openCurrentAccount(String customerId, double initialDeposit, double overdraftLimit) throws BankingException;

    /**
     * Locates a bank account by its unique account number.
     *
     * @param accountNumber The account number to look up
     * @return The matching {@link BankAccount}
     * @throws AccountNotFoundException if no such account exists
     */
    BankAccount findAccount(String accountNumber) throws AccountNotFoundException;

    /**
     * Locates a customer by their unique customer ID.
     *
     * @param customerId The customer ID to look up
     * @return The matching {@link Customer}
     * @throws CustomerNotFoundException if no such customer exists
     */
    Customer findCustomer(String customerId) throws CustomerNotFoundException;

    /**
     * Performs a standard cash deposit into the specified account.
     *
     * @param accountNumber Target account number
     * @param amount        Deposit amount
     * @throws BankingException on account not found or invalid amount
     */
    void deposit(String accountNumber, double amount) throws BankingException;

    /**
     * Performs an overloaded deposit with a custom note.
     *
     * @param accountNumber Target account number
     * @param amount        Deposit amount
     * @param note          Memo / narration
     * @throws BankingException on account not found or invalid amount
     */
    void deposit(String accountNumber, double amount, String note) throws BankingException;

    /**
     * Withdraws funds from the specified account according to its domain rules.
     *
     * @param accountNumber Target account number
     * @param amount        Withdrawal amount
     * @throws BankingException on insufficient funds, overdraft breach, or account not found
     */
    void withdraw(String accountNumber, double amount) throws BankingException;

    /**
     * Executes a thread-safe, deadlock-free inter-account transfer.
     *
     * @param fromAccNum    Originating account number
     * @param toAccNum      Beneficiary account number
     * @param amount        Transfer amount
     * @param description   Transfer memo/narration
     * @throws BankingException on account not found, self-transfer, or insufficient funds
     */
    void transfer(String fromAccNum, String toAccNum, double amount, String description) throws BankingException;

    /**
     * Retrieves an immutable list of all registered accounts.
     *
     * @return Snapshot list of accounts
     */
    List<BankAccount> getAllAccounts();

    /**
     * Retrieves an immutable list of all registered customers.
     *
     * @return Snapshot list of customers
     */
    List<Customer> getAllCustomers();

    /**
     * Retrieves all accounts associated with a specific customer.
     *
     * @param customerId Unique customer ID
     * @return List of bank accounts owned by the customer
     * @throws CustomerNotFoundException if customer does not exist
     */
    List<BankAccount> getAccountsForCustomer(String customerId) throws CustomerNotFoundException;

    /**
     * Triggers interest calculation and credit across all savings accounts.
     */
    void applyInterestToSavingsAccounts();
}
