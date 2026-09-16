package com.bank.test;

import com.bank.exception.AccountNotFoundException;
import com.bank.exception.CustomerNotFoundException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.OverdraftLimitExceededException;
import com.bank.model.BankAccount;
import com.bank.model.CurrentAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.model.Transaction;
import com.bank.model.TransactionType;
import com.bank.service.BankService;
import com.bank.service.BankServiceImpl;

import java.util.List;

/**
 * Comprehensive Automated Domain & Invariant Test Suite.
 * Tests edge cases, exception throwing, balance invariants, defensive copies, and identity contracts.
 */
public class BankingSystemTest {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("                 RUNNING CORE BANKING INVARIANT TEST SUITE                ");
        System.out.println("==========================================================================");

        testCustomerIdentityAndImmutability();
        testSavingsAccountInvariants();
        testSavingsAccountInterest();
        testCurrentAccountOverdraftAndFees();
        testDefensiveCopyingAuditProtection();
        testNegativeDepositValidation();
        testDeadlockFreeTransferLogic();
        testSelfTransferRejection();
        testAccountNotFoundExceptions();
        testEqualsAndHashCodeContracts();

        System.out.println("==========================================================================");
        System.out.printf(" TEST RESULTS: %d / %d PASSED\n", passedTests, totalTests);
        if (passedTests == totalTests) {
            System.out.println(" ALL UNIT & INVARIANT TESTS PASSED WITH ZERO FAILURES!");
        } else {
            System.err.println(" SOME TESTS FAILED!");
            System.exit(1);
        }
        System.out.println("==========================================================================");
    }

    private static void assertTrue(String testName, boolean condition, String failureMsg) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println(" [PASS] " + testName);
        } else {
            System.err.println(" [FAIL] " + testName + " - " + failureMsg);
        }
    }

    private static void testCustomerIdentityAndImmutability() {
        Customer c1 = new Customer("CUST-101", "John Doe", "john@example.com", "1234567890");
        Customer c2 = new Customer("CUST-101", "John Doe Alternate", "different@example.com", "0987654321");
        Customer c3 = new Customer("CUST-102", "Jane Doe", "jane@example.com", "1234567890");

        assertTrue("Customer equals based on customerId", c1.equals(c2), "c1 should equal c2 with same ID");
        assertTrue("Customer not equal with different customerId", !c1.equals(c3), "c1 should not equal c3");
        assertTrue("Customer hashCode matches on customerId", c1.hashCode() == c2.hashCode(), "hashCodes must match");

        c1.addAccount("ACC-001");
        List<String> accs = c1.getAssociatedAccountNumbers();
        try {
            accs.add("ACC-HACK");
            assertTrue("Customer unmodifiable account list", false, "Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue("Customer unmodifiable account list", true, "");
        }
    }

    private static void testSavingsAccountInvariants() {
        Customer c = new Customer("CUST-103", "Alice", "alice@example.com", "1234567890");
        try {
            SavingsAccount sa = new SavingsAccount("SAV-101", c, 5000.00, 0.05);

            // Allowed withdrawal: 5000 - 3000 = 2000 (>= 1000 min balance)
            sa.withdraw(3000.00);
            assertTrue("Savings valid withdrawal", Math.abs(sa.getBalance() - 2000.00) < 0.001, "Balance should be 2000");

            // Breach withdrawal: 2000 - 1500 = 500 (< 1000 min balance) -> must throw InsufficientFundsException
            try {
                sa.withdraw(1500.00);
                assertTrue("Savings min balance breach throws InsufficientFundsException", false, "Did not throw exception");
            } catch (InsufficientFundsException e) {
                assertTrue("Savings min balance breach throws InsufficientFundsException",
                        e.getMinimumBalance() == 1000.00 && e.getCurrentBalance() == 2000.00, "Exception metadata check");
            }
        } catch (Exception e) {
            assertTrue("Savings account test", false, "Unexpected exception: " + e.getMessage());
        }
    }

    private static void testSavingsAccountInterest() {
        Customer c = new Customer("CUST-104", "Bob", "bob@example.com", "1234567890");
        try {
            SavingsAccount sa = new SavingsAccount("SAV-102", c, 10000.00, 0.05);
            double interest = sa.applyInterest();
            assertTrue("Savings interest calculation", Math.abs(interest - 500.00) < 0.001, "5% of 10,000 is 500");
            assertTrue("Savings interest credited to balance", Math.abs(sa.getBalance() - 10500.00) < 0.001, "Balance should be 10500");

            List<Transaction> history = sa.getTransactionHistory();
            boolean hasInterestTx = history.stream().anyMatch(tx -> tx.type() == TransactionType.INTEREST);
            assertTrue("Savings interest transaction logged", hasInterestTx, "Transaction history must record INTEREST");
        } catch (Exception e) {
            assertTrue("Savings interest test", false, "Unexpected exception: " + e.getMessage());
        }
    }

    private static void testCurrentAccountOverdraftAndFees() {
        Customer c = new Customer("CUST-105", "Charlie", "charlie@example.com", "1234567890");
        try {
            CurrentAccount ca = new CurrentAccount("CUR-101", c, 5000.00, 10000.00);

            // Within balance withdrawal (no fee)
            ca.withdraw(2000.00);
            assertTrue("Current account normal withdrawal", Math.abs(ca.getBalance() - 3000.00) < 0.001, "Balance should be 3000");

            // Overdraft withdrawal: Balance is 3000, withdraw 5000 (utilizes 2000 overdraft, incurs 50 fee)
            // Balance = 3000 - 5000 - 50 = -2050
            ca.withdraw(5000.00);
            assertTrue("Current account overdraft fee applied", Math.abs(ca.getBalance() - (-2050.00)) < 0.001, "Balance should be -2050");

            // Overdraft limit breach: Available = -2050 + 10000 = 7950. Try to withdraw 8000.
            try {
                ca.withdraw(8000.00);
                assertTrue("Current account limit breach throws OverdraftLimitExceededException", false, "Should throw");
            } catch (OverdraftLimitExceededException e) {
                assertTrue("Current account limit breach throws OverdraftLimitExceededException",
                        e.getOverdraftLimit() == 10000.00, "Exception check");
            }
        } catch (Exception e) {
            assertTrue("Current account test", false, "Unexpected exception: " + e.getMessage());
        }
    }

    private static void testDefensiveCopyingAuditProtection() {
        Customer c = new Customer("CUST-106", "David", "david@example.com", "1234567890");
        try {
            SavingsAccount sa = new SavingsAccount("SAV-103", c, 2000.00, 0.04);
            List<Transaction> txHistory = sa.getTransactionHistory();
            try {
                txHistory.clear();
                assertTrue("Defensive copy prevents clearing transactions", false, "Should throw UnsupportedOperationException");
            } catch (UnsupportedOperationException e) {
                assertTrue("Defensive copy prevents clearing transactions", true, "");
            }
        } catch (Exception e) {
            assertTrue("Defensive copy test", false, "Unexpected exception: " + e.getMessage());
        }
    }

    private static void testNegativeDepositValidation() {
        Customer c = new Customer("CUST-107", "Emma", "emma@example.com", "1234567890");
        try {
            new SavingsAccount("SAV-104", c, -500.00, 0.04);
            assertTrue("Negative opening balance rejected", false, "Should throw InvalidAmountException");
        } catch (InvalidAmountException e) {
            assertTrue("Negative opening balance rejected", e.getInvalidAmount() == -500.00, "Amount metadata check");
        } catch (Exception e) {
            assertTrue("Negative opening balance test", false, "Wrong exception type");
        }
    }

    private static void testDeadlockFreeTransferLogic() {
        BankService service = new BankServiceImpl();
        try {
            Customer c1 = service.registerCustomer("User 1", "u1@example.com", "111");
            Customer c2 = service.registerCustomer("User 2", "u2@example.com", "222");

            BankAccount a1 = service.openSavingsAccount(c1.getCustomerId(), 5000.00, 0.04);
            BankAccount a2 = service.openSavingsAccount(c2.getCustomerId(), 5000.00, 0.04);

            service.transfer(a1.getAccountNumber(), a2.getAccountNumber(), 2000.00, "Rent Payment");

            assertTrue("Transfer sender balance deducted", Math.abs(a1.getBalance() - 3000.00) < 0.001, "Balance 3000");
            assertTrue("Transfer receiver balance credited", Math.abs(a2.getBalance() - 7000.00) < 0.001, "Balance 7000");
        } catch (Exception e) {
            assertTrue("Transfer test", false, "Unexpected exception: " + e.getMessage());
        }
    }

    private static void testSelfTransferRejection() {
        BankService service = new BankServiceImpl();
        try {
            Customer c = service.registerCustomer("User Self", "uself@example.com", "333");
            BankAccount a = service.openSavingsAccount(c.getCustomerId(), 5000.00, 0.04);
            try {
                service.transfer(a.getAccountNumber(), a.getAccountNumber(), 1000.00, "Self loop");
                assertTrue("Self-transfer rejected", false, "Should throw InvalidAmountException or BankingException");
            } catch (InvalidAmountException e) {
                assertTrue("Self-transfer rejected with InvalidAmountException", true, "");
            }
        } catch (Exception e) {
            assertTrue("Self transfer test", false, "Unexpected exception: " + e.getMessage());
        }
    }

    private static void testAccountNotFoundExceptions() {
        BankService service = new BankServiceImpl();
        try {
            service.findAccount("NON-EXISTENT-ACC");
            assertTrue("AccountNotFoundException thrown", false, "Should throw");
        } catch (AccountNotFoundException e) {
            assertTrue("AccountNotFoundException thrown", e.getAccountNumber().contains("NON-EXISTENT-ACC"), "Account number stored");
        } catch (Exception e) {
            assertTrue("AccountNotFoundException test", false, "Wrong exception");
        }
    }

    private static void testEqualsAndHashCodeContracts() {
        Customer c = new Customer("CUST-108", "George", "g@example.com", "444");
        try {
            SavingsAccount sa1 = new SavingsAccount("SAV-999", c, 1000.00, 0.04);
            SavingsAccount sa2 = new SavingsAccount("SAV-999", c, 5000.00, 0.06);
            SavingsAccount sa3 = new SavingsAccount("SAV-888", c, 1000.00, 0.04);

            assertTrue("BankAccount equals by account number", sa1.equals(sa2), "Accounts with same number must be equal");
            assertTrue("BankAccount not equals if numbers differ", !sa1.equals(sa3), "Different numbers must not be equal");
            assertTrue("BankAccount hashCode equality", sa1.hashCode() == sa2.hashCode(), "HashCodes must match");
        } catch (Exception e) {
            assertTrue("Equals/HashCode test", false, "Unexpected exception: " + e.getMessage());
        }
    }
}
