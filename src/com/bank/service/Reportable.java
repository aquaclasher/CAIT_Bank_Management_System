package com.bank.service;

import com.bank.model.Transaction;
import java.util.List;

/**
 * Contract for account ledger auditing, statement generation, and passbook reporting.
 */
public interface Reportable {

    /**
     * Retrieves an immutable snapshot of all recorded transactions for auditing.
     *
     * @return Defensive unmodifiable list of historical transactions
     */
    List<Transaction> getTransactionHistory();

    /**
     * Generates a fully formatted, human-readable bank statement / passbook summary.
     *
     * @return Formatted multi-line statement
     */
    String generateStatement();
}
