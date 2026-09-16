package com.bank.ui;

import com.bank.concurrency.ConcurrencyHarness;
import com.bank.exception.BankingException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.OverdraftLimitExceededException;
import com.bank.model.BankAccount;
import com.bank.model.Customer;
import com.bank.model.SavingsAccount;
import com.bank.model.CurrentAccount;
import com.bank.service.BankService;
import com.bank.service.BankServiceImpl;

import java.util.List;
import java.util.Scanner;

/**
 * Interactive Command Line Interface for the Core Banking Management System.
 * Features defensive input handling, formatted tabular reports, and seamless
 * execution of domain operations and stress tests.
 */
public class BankApplicationCLI {
    private final BankService bankService;
    private final Scanner scanner;

    public BankApplicationCLI() {
        this.bankService = new BankServiceImpl();
        this.scanner = new Scanner(System.in);
        seedInitialData();
    }

    public BankApplicationCLI(BankService bankService, Scanner scanner) {
        this.bankService = bankService;
        this.scanner = scanner;
    }

    /**
     * Pre-seeds initial customers and accounts for immediate exploration and testing.
     */
    private void seedInitialData() {
        try {
            Customer c1 = bankService.registerCustomer("Vikram Sharma", "vikram.sharma@example.com", "+91-9876543210");
            Customer c2 = bankService.registerCustomer("Priya Patel", "priya.patel@example.com", "+91-9876543211");
            Customer c3 = bankService.registerCustomer("Enterprise Ltd", "accounts@enterpriseltd.in", "+91-9876543212");

            bankService.openSavingsAccount(c1.getCustomerId(), 25000.00, 0.04);
            bankService.openSavingsAccount(c2.getCustomerId(), 15000.00, 0.04);
            bankService.openCurrentAccount(c3.getCustomerId(), 50000.00, 50000.00);
        } catch (Exception e) {
            // Seed data initialization
        }
    }

    public static void main(String[] args) {
        BankApplicationCLI app = new BankApplicationCLI();
        app.start();
    }

    /**
     * Main CLI loop.
     */
    public void start() {
        printHeader();
        boolean running = true;

        while (running) {
            printMainMenu();
            int choice = readInt("Select an option [1-10]: ", 1, 10);
            System.out.println();

            switch (choice) {
                case 1 -> handleRegisterCustomer();
                case 2 -> handleOpenSavingsAccount();
                case 3 -> handleOpenCurrentAccount();
                case 4 -> handleDeposit();
                case 5 -> handleWithdraw();
                case 6 -> handleTransfer();
                case 7 -> handleViewStatement();
                case 8 -> handleViewCustomerProfile();
                case 9 -> handleRunConcurrencyHarness();
                case 10 -> {
                    System.out.println("==========================================================================");
                    System.out.println(" Thank you for using the Core Banking Management System. Goodbye!");
                    System.out.println("==========================================================================");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please try again.");
            }
            if (running) {
                System.out.println("\nPress ENTER to return to the main menu...");
                scanner.nextLine();
            }
        }
    }

    private void printHeader() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║            RETAIL BANKING CORE MANAGEMENT SYSTEM (JAVA 21)             ║");
        System.out.println("║          Thread-Safe • Deadlock-Free • Production Domain Core          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝");
    }

    private void printMainMenu() {
        System.out.println("\n╔══════════════════════════════ MAIN MENU ══════════════════════════════╗");
        System.out.println("║  1. Register New Customer                                              ║");
        System.out.println("║  2. Open Savings Account (₹1,000 Min Balance + Interest Accrual)      ║");
        System.out.println("║  3. Open Current Account (Overdraft Facility + Auto Fee Deduction)     ║");
        System.out.println("║  4. Deposit Funds (Standard / Custom Memo)                             ║");
        System.out.println("║  5. Withdraw Funds (ATM / Counter Debit)                               ║");
        System.out.println("║  6. Inter-Account Transfer (Deadlock-Free Atomic Transfer)             ║");
        System.out.println("║  7. View Account Statement / Passbook Ledger                           ║");
        System.out.println("║  8. View Customer Profile & Associated Accounts                        ║");
        System.out.println("║  9. Run Multithreading & Concurrency Stress Test                       ║");
        System.out.println("║ 10. Exit System                                                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝");
    }

    private void handleRegisterCustomer() {
        System.out.println("--- [1] REGISTER NEW CUSTOMER ---");
        String name = readNonEmptyString("Enter Full Name: ");
        String email = readNonEmptyString("Enter Email Address: ");
        String phone = readNonEmptyString("Enter Phone Number: ");

        try {
            Customer customer = bankService.registerCustomer(name, email, phone);
            System.out.println("\n✔ SUCCESS: Customer registered successfully!");
            System.out.printf("  Customer ID : %s\n", customer.getCustomerId());
            System.out.printf("  Full Name   : %s\n", customer.getFullName());
            System.out.printf("  Email       : %s\n", customer.getEmail());
            System.out.printf("  Phone       : %s\n", customer.getPhone());
        } catch (BankingException e) {
            printError("Customer Registration Failed", e.getMessage());
        }
    }

    private void handleOpenSavingsAccount() {
        System.out.println("--- [2] OPEN SAVINGS ACCOUNT ---");
        displayAllCustomersShort();
        String custId = readNonEmptyString("Enter Customer ID: ");
        double initialDeposit = readDouble("Enter Initial Deposit (Min ₹1,000 recommended, >= 0 allowed): ", 0.0);
        double interestRate = readDouble("Enter Annual Interest Rate (e.g. 0.04 for 4.0%): ", 0.0);

        try {
            BankAccount account = bankService.openSavingsAccount(custId, initialDeposit, interestRate);
            System.out.println("\n✔ SUCCESS: Savings Account opened successfully!");
            System.out.printf("  Account Number : %s\n", account.getAccountNumber());
            System.out.printf("  Primary Holder : %s (%s)\n", account.getOwner().getFullName(), custId);
            System.out.printf("  Initial Balance: ₹%,.2f\n", account.getBalance());
            System.out.printf("  Interest Rate  : %.2f%%\n", ((SavingsAccount) account).getInterestRate() * 100.0);
            System.out.printf("  Minimum Balance: ₹%,.2f\n", SavingsAccount.MINIMUM_BALANCE);
        } catch (BankingException e) {
            printError("Account Opening Failed", e.getMessage());
        }
    }

    private void handleOpenCurrentAccount() {
        System.out.println("--- [3] OPEN CURRENT ACCOUNT ---");
        displayAllCustomersShort();
        String custId = readNonEmptyString("Enter Customer ID: ");
        double initialDeposit = readDouble("Enter Initial Deposit (>= 0): ", 0.0);
        double overdraftLimit = readDouble("Enter Sanctioned Overdraft Limit (e.g. 25000.00): ", 0.0);

        try {
            BankAccount account = bankService.openCurrentAccount(custId, initialDeposit, overdraftLimit);
            System.out.println("\n✔ SUCCESS: Current Account opened successfully!");
            System.out.printf("  Account Number : %s\n", account.getAccountNumber());
            System.out.printf("  Primary Holder : %s (%s)\n", account.getOwner().getFullName(), custId);
            System.out.printf("  Initial Balance: ₹%,.2f\n", account.getBalance());
            System.out.printf("  Overdraft Limit: ₹%,.2f\n", ((CurrentAccount) account).getOverdraftLimit());
            System.out.printf("  Overdraft Fee  : ₹%,.2f per overdraft debit\n", CurrentAccount.OVERDRAFT_FEE);
        } catch (BankingException e) {
            printError("Account Opening Failed", e.getMessage());
        }
    }

    private void handleDeposit() {
        System.out.println("--- [4] DEPOSIT FUNDS ---");
        displayAllAccountsShort();
        String accNum = readNonEmptyString("Enter Target Account Number: ");
        double amount = readDouble("Enter Deposit Amount (₹): ", 0.01);
        System.out.print("Enter Custom Narration / Note (or press ENTER for default): ");
        String note = scanner.nextLine().trim();

        try {
            if (note.isEmpty()) {
                bankService.deposit(accNum, amount);
            } else {
                bankService.deposit(accNum, amount, note);
            }
            BankAccount account = bankService.findAccount(accNum);
            System.out.println("\n✔ SUCCESS: Deposit credited successfully!");
            System.out.printf("  Account Number: %s\n", account.getAccountNumber());
            System.out.printf("  Amount Credited: ₹%,.2f\n", amount);
            System.out.printf("  Updated Balance: ₹%,.2f\n", account.getBalance());
        } catch (BankingException e) {
            printError("Deposit Failed", e.getMessage());
        }
    }

    private void handleWithdraw() {
        System.out.println("--- [5] WITHDRAW FUNDS ---");
        displayAllAccountsShort();
        String accNum = readNonEmptyString("Enter Account Number: ");
        double amount = readDouble("Enter Withdrawal Amount (₹): ", 0.01);

        try {
            bankService.withdraw(accNum, amount);
            BankAccount account = bankService.findAccount(accNum);
            System.out.println("\n✔ SUCCESS: Withdrawal completed successfully!");
            System.out.printf("  Account Number: %s\n", account.getAccountNumber());
            System.out.printf("  Amount Debited: ₹%,.2f\n", amount);
            System.out.printf("  Updated Balance: ₹%,.2f\n", account.getBalance());
        } catch (InsufficientFundsException e) {
            printError("Insufficient Funds Error", String.format(
                    "Cannot complete withdrawal. Current Balance: ₹%,.2f, Requested: ₹%,.2f, Minimum Balance Rule: ₹%,.2f",
                    e.getCurrentBalance(), e.getRequestedAmount(), e.getMinimumBalance()));
        } catch (OverdraftLimitExceededException e) {
            printError("Overdraft Limit Exceeded", String.format(
                    "Cannot complete withdrawal. Current Balance: ₹%,.2f, Overdraft Limit: ₹%,.2f (Max Available: ₹%,.2f), Requested: ₹%,.2f",
                    e.getCurrentBalance(), e.getOverdraftLimit(), e.getCurrentBalance() + e.getOverdraftLimit(), e.getRequestedAmount()));
        } catch (BankingException e) {
            printError("Withdrawal Failed", e.getMessage());
        }
    }

    private void handleTransfer() {
        System.out.println("--- [6] INTER-ACCOUNT TRANSFER (DEADLOCK-FREE) ---");
        displayAllAccountsShort();
        String fromAccNum = readNonEmptyString("Enter Source (From) Account Number: ");
        String toAccNum = readNonEmptyString("Enter Destination (To) Account Number: ");
        double amount = readDouble("Enter Transfer Amount (₹): ", 0.01);
        System.out.print("Enter Transfer Memo / Description: ");
        String desc = scanner.nextLine().trim();

        try {
            bankService.transfer(fromAccNum, toAccNum, amount, desc);
            BankAccount fromAcc = bankService.findAccount(fromAccNum);
            BankAccount toAcc = bankService.findAccount(toAccNum);

            System.out.println("\n✔ SUCCESS: Inter-account transfer completed atomically!");
            System.out.printf("  Transferred Amount : ₹%,.2f\n", amount);
            System.out.printf("  From Account (%s) : New Balance: ₹%,.2f\n", fromAcc.getAccountNumber(), fromAcc.getBalance());
            System.out.printf("  To Account (%s)   : New Balance: ₹%,.2f\n", toAcc.getAccountNumber(), toAcc.getBalance());
        } catch (InsufficientFundsException e) {
            printError("Transfer Aborted - Insufficient Funds", String.format(
                    "Sender %s does not have enough funds. Balance: ₹%,.2f, Requested: ₹%,.2f, Min Balance: ₹%,.2f",
                    fromAccNum, e.getCurrentBalance(), e.getRequestedAmount(), e.getMinimumBalance()));
        } catch (OverdraftLimitExceededException e) {
            printError("Transfer Aborted - Overdraft Exceeded", String.format(
                    "Sender %s exceeded credit line. Balance: ₹%,.2f, Overdraft Limit: ₹%,.2f, Requested: ₹%,.2f",
                    fromAccNum, e.getCurrentBalance(), e.getOverdraftLimit(), e.getRequestedAmount()));
        } catch (BankingException e) {
            printError("Transfer Failed", e.getMessage());
        }
    }

    private void handleViewStatement() {
        System.out.println("--- [7] VIEW ACCOUNT STATEMENT / PASSBOOK ---");
        displayAllAccountsShort();
        String accNum = readNonEmptyString("Enter Account Number: ");

        try {
            BankAccount account = bankService.findAccount(accNum);
            System.out.println("\n" + account.generateStatement());
        } catch (BankingException e) {
            printError("Statement Retrieval Failed", e.getMessage());
        }
    }

    private void handleViewCustomerProfile() {
        System.out.println("--- [8] VIEW CUSTOMER PROFILE & ACCOUNTS ---");
        displayAllCustomersShort();
        String custId = readNonEmptyString("Enter Customer ID: ");

        try {
            Customer customer = bankService.findCustomer(custId);
            List<BankAccount> accounts = bankService.getAccountsForCustomer(custId);

            System.out.println("==========================================================================");
            System.out.println("                           CUSTOMER PROFILE REPORT                        ");
            System.out.println("==========================================================================");
            System.out.printf(" Customer ID : %-20s Full Name : %s\n", customer.getCustomerId(), customer.getFullName());
            System.out.printf(" Email       : %-20s Phone     : %s\n", customer.getEmail(), customer.getPhone());
            System.out.println("--------------------------------------------------------------------------");
            System.out.println(" ASSOCIATED BANK ACCOUNTS:");
            if (accounts.isEmpty()) {
                System.out.println("  No active bank accounts linked to this customer.");
            } else {
                System.out.printf("  %-12s | %-16s | %-15s | %s\n", "ACCOUNT NO", "TYPE", "BALANCE", "TRANSACTIONS");
                System.out.println("  ------------------------------------------------------------------------");
                double totalBalance = 0.0;
                for (BankAccount acc : accounts) {
                    System.out.printf("  %-12s | %-16s | ₹%,13.2f | %d items\n",
                            acc.getAccountNumber(),
                            acc.getAccountType(),
                            acc.getBalance(),
                            acc.getTransactionHistory().size());
                    totalBalance += acc.getBalance();
                }
                System.out.println("  ------------------------------------------------------------------------");
                System.out.printf("  TOTAL RELATIONSHIP VALUE ACROSS ALL ACCOUNTS: ₹%,.2f\n", totalBalance);
            }
            System.out.println("==========================================================================");
        } catch (BankingException e) {
            printError("Profile Query Failed", e.getMessage());
        }
    }

    private void handleRunConcurrencyHarness() {
        System.out.println("--- [9] MULTITHREADING & CONCURRENCY STRESS TESTS ---");
        System.out.println("Executing automated multithreading test suite...");
        System.out.println();
        ConcurrencyHarness.main(new String[0]);
    }

    private void displayAllCustomersShort() {
        List<Customer> customers = bankService.getAllCustomers();
        if (customers.isEmpty()) return;
        System.out.println(" [Registered Customers: " +
                customers.stream().map(c -> c.getCustomerId() + " (" + c.getFullName() + ")").toList() + "]");
    }

    private void displayAllAccountsShort() {
        List<BankAccount> accounts = bankService.getAllAccounts();
        if (accounts.isEmpty()) return;
        System.out.println(" [Active Accounts: " +
                accounts.stream().map(a -> a.getAccountNumber() + " [₹" + String.format("%.2f", a.getBalance()) + "]").toList() + "]");
    }

    private void printError(String title, String message) {
        System.out.println("\n✘ ERROR: " + title);
        System.out.println("  Details: " + message);
    }

    // Defensive input readers
    private String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Input cannot be empty. Please enter a valid value.");
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("Please enter a number between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric input. Please enter an integer.");
            }
        }
    }

    private double readDouble(String prompt, double min) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value >= min) {
                    return value;
                }
                System.out.printf("Please enter a value greater than or equal to %.2f.\n", min);
            } catch (NumberFormatException e) {
                System.out.println("Invalid decimal input. Please enter a valid number (e.g., 1000.50).");
            }
        }
    }
}
