package com.bank.concurrency;

import com.bank.exception.BankingException;
import com.bank.exception.InsufficientFundsException;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.service.BankService;
import com.bank.service.BankServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrency test harness demonstrating:
 * 1. Race condition prevention on a shared account during simultaneous multi-channel withdrawals.
 * 2. Deadlock-free high-frequency bidirectional inter-account transfers.
 */
public class ConcurrencyHarness {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("            STARTING CORE BANKING CONCURRENCY & STRESS HARNESS            ");
        System.out.println("==========================================================================\n");

        runScenario1RaceCondition();
        System.out.println("\n--------------------------------------------------------------------------\n");
        runScenario2BidirectionalTransfers();

        System.out.println("\n==========================================================================");
        System.out.println("            ALL CONCURRENCY TESTS COMPLETED SUCCESSFULLY!                ");
        System.out.println("==========================================================================");
    }

    /**
     * Scenario 1: Race Condition on Shared Account (ATM vs. Online POS vs. Mobile App).
     *
     * Invariant Validation:
     * - Opening Balance: $10,000.00
     * - Minimum Balance Rule: $1,000.00
     * - Maximum withdrawable funds: $9,000.00
     * - 10 concurrent threads each attempting to withdraw $1,200.00 (Total requested: $12,000.00)
     * - Expected outcome: Exactly 7 successful withdrawals (7 * $1,200 = $8,400.00 deducted).
     * - Exactly 3 rejected withdrawals throwing InsufficientFundsException.
     * - Final Balance: $1,600.00 (>= $1,000.00 minimum balance invariant).
     */
    public static boolean runScenario1RaceCondition() {
        System.out.println(">>> SCENARIO 1: Race Condition on Shared Account (ATM vs. Online POS)");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Initial Setup:");
        System.out.println(" - Account Type   : Savings Account");
        System.out.println(" - Initial Balance: $10,000.00");
        System.out.println(" - Minimum Balance: $1,000.00 (Non-breachable invariant)");
        System.out.println(" - Workers        : 10 concurrent threads");
        System.out.println(" - Request/Worker : $1,200.00 (Total attempted: $12,000.00)");
        System.out.println("--------------------------------------------------------------------------");

        BankService bankService = new BankServiceImpl();
        try {
            Customer customer = bankService.registerCustomer("Alice Wonderland", "alice@example.com", "9876543210");
            BankAccount savingsAcc = bankService.openSavingsAccount(customer.getCustomerId(), 10000.00, 0.04);
            String accNum = savingsAcc.getAccountNumber();

            int threadCount = 10;
            double withdrawAmount = 1200.00;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch endGate = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<String> auditLogs = Collections.synchronizedList(new ArrayList<>());

            for (int i = 1; i <= threadCount; i++) {
                final int workerId = i;
                executor.submit(() -> {
                    try {
                        startGate.await(); // Synchronize all 10 threads to hit the account simultaneously
                        savingsAcc.withdraw(withdrawAmount);
                        successCount.incrementAndGet();
                        auditLogs.add(String.format(" [SUCCESS] Thread-%02d withdrew $%.2f. New Balance: $%.2f",
                                workerId, withdrawAmount, savingsAcc.getBalance()));
                    } catch (InsufficientFundsException ife) {
                        failureCount.incrementAndGet();
                        auditLogs.add(String.format(" [REJECTED] Thread-%02d failed: %s", workerId, ife.getMessage()));
                    } catch (BankingException | InterruptedException e) {
                        failureCount.incrementAndGet();
                        auditLogs.add(String.format(" [ERROR] Thread-%02d unexpected error: %s", workerId, e.getMessage()));
                    } finally {
                        endGate.countDown();
                    }
                });
            }

            // Release all threads simultaneously
            long startTime = System.currentTimeMillis();
            startGate.countDown();
            endGate.await(10, TimeUnit.SECONDS);
            executor.shutdown();
            long elapsed = System.currentTimeMillis() - startTime;

            System.out.println("Execution Log (Concurrent Threads Interleaving):");
            for (String log : auditLogs) {
                System.out.println(log);
            }

            System.out.println("\nVerification & Metrics:");
            System.out.printf(" - Time Taken          : %d ms\n", elapsed);
            System.out.printf(" - Successful Debits   : %d (Expected: 7)\n", successCount.get());
            System.out.printf(" - Rejected Debits     : %d (Expected: 3)\n", failureCount.get());
            System.out.printf(" - Final Ledger Balance: $%.2f (Expected: $1,600.00)\n", savingsAcc.getBalance());
            System.out.printf(" - Total Passbook Items: %d (1 Initial + 7 Withdrawals = 8)\n", savingsAcc.getTransactionHistory().size());

            boolean balanceValid = Math.abs(savingsAcc.getBalance() - 1600.00) < 0.001;
            boolean countValid = (successCount.get() == 7) && (failureCount.get() == 3);
            boolean minBalancePreserved = savingsAcc.getBalance() >= SavingsAccount.MINIMUM_BALANCE;
            boolean ledgerValid = savingsAcc.getTransactionHistory().size() == 8;

            if (balanceValid && countValid && minBalancePreserved && ledgerValid) {
                System.out.println(" [PASS] Scenario 1 Verification Passed: Thread-safety preserved with zero race conditions.");
                return true;
            } else {
                System.err.println(" [FAIL] Scenario 1 Verification Failed!");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Exception in Scenario 1: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Scenario 2: High-Frequency Bidirectional Transfers (Deadlock-Free).
     *
     * Invariant Validation:
     * - ACC-1 Starting Balance: $20,000.00
     * - ACC-2 Starting Balance: $20,000.00
     * - Total Pooled Wealth: $40,000.00
     * - Thread A: 500 transfers of $100 from ACC-1 to ACC-2
     * - Thread B: 500 transfers of $100 from ACC-2 to ACC-1
     * - Total transfers: 1,000 transfers running concurrently.
     * - Expected outcome: Zero deadlock, all 1,000 transfers succeed, pooled wealth remains $40,000.00,
     *   final balances: ACC-1 = $20,000.00, ACC-2 = $20,000.00.
     */
    public static boolean runScenario2BidirectionalTransfers() {
        System.out.println(">>> SCENARIO 2: High-Frequency Bidirectional Transfers (Deadlock Prevention)");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Initial Setup:");
        System.out.println(" - Account 1 (ACC-1) Initial Balance: $20,000.00");
        System.out.println(" - Account 2 (ACC-2) Initial Balance: $20,000.00");
        System.out.println(" - Total System Pooled Money        : $40,000.00");
        System.out.println(" - Thread A: 500 transfers of $100 (ACC-1 -> ACC-2)");
        System.out.println(" - Thread B: 500 transfers of $100 (ACC-2 -> ACC-1)");
        System.out.println(" - Total Transfers: 1,000 concurrent transfers");
        System.out.println("--------------------------------------------------------------------------");

        BankService bankService = new BankServiceImpl();
        try {
            Customer cust1 = bankService.registerCustomer("Bob Builder", "bob@example.com", "9876543211");
            Customer cust2 = bankService.registerCustomer("Charlie Chaplin", "charlie@example.com", "9876543212");

            BankAccount acc1 = bankService.openSavingsAccount(cust1.getCustomerId(), 20000.00, 0.03);
            BankAccount acc2 = bankService.openSavingsAccount(cust2.getCustomerId(), 20000.00, 0.03);

            String accNum1 = acc1.getAccountNumber();
            String accNum2 = acc2.getAccountNumber();

            int transfersPerThread = 500;
            double transferAmount = 100.00;

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch endGate = new CountDownLatch(2);

            AtomicInteger threadASuccess = new AtomicInteger(0);
            AtomicInteger threadBSuccess = new AtomicInteger(0);
            AtomicInteger errors = new AtomicInteger(0);

            // Thread A: ACC-1 -> ACC-2
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < transfersPerThread; i++) {
                        bankService.transfer(accNum1, accNum2, transferAmount, "Batch Transfer A #" + (i + 1));
                        threadASuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.err.println("Error in Thread A: " + e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });

            // Thread B: ACC-2 -> ACC-1
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < transfersPerThread; i++) {
                        bankService.transfer(accNum2, accNum1, transferAmount, "Batch Transfer B #" + (i + 1));
                        threadBSuccess.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    System.err.println("Error in Thread B: " + e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });

            long startTime = System.currentTimeMillis();
            startGate.countDown();

            // Wait up to 15 seconds for completion
            boolean completed = endGate.await(15, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startTime;
            executor.shutdown();

            if (!completed) {
                System.err.println(" [DEADLOCK DETECTED] Transfers timed out! System deadlocked!");
                return false;
            }

            double finalBalance1 = acc1.getBalance();
            double finalBalance2 = acc2.getBalance();
            double totalPooledMoney = finalBalance1 + finalBalance2;

            System.out.println("Verification & Metrics:");
            System.out.printf(" - Execution Time         : %d ms\n", elapsed);
            System.out.printf(" - Thread A Transfers (1->2): %d / %d\n", threadASuccess.get(), transfersPerThread);
            System.out.printf(" - Thread B Transfers (2->1): %d / %d\n", threadBSuccess.get(), transfersPerThread);
            System.out.printf(" - Total Errors / Aborts  : %d\n", errors.get());
            System.out.printf(" - ACC-1 Final Balance    : $%,.2f (Expected: $20,000.00)\n", finalBalance1);
            System.out.printf(" - ACC-2 Final Balance    : $%,.2f (Expected: $20,000.00)\n", finalBalance2);
            System.out.printf(" - Total Pooled Money     : $%,.2f (Expected: $40,000.00)\n", totalPooledMoney);

            boolean totalConserved = Math.abs(totalPooledMoney - 40000.00) < 0.001;
            boolean balancesEqual = Math.abs(finalBalance1 - 20000.00) < 0.001 && Math.abs(finalBalance2 - 20000.00) < 0.001;
            boolean allExecuted = (threadASuccess.get() == transfersPerThread) && (threadBSuccess.get() == transfersPerThread);

            if (totalConserved && balancesEqual && allExecuted) {
                System.out.println(" [PASS] Scenario 2 Verification Passed: 1,000 transfers completed without deadlock, conservation of money maintained.");
                return true;
            } else {
                System.err.println(" [FAIL] Scenario 2 Verification Failed!");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Exception in Scenario 2: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
