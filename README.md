# Multi-Channel Retail Banking Core Management System (Java 21)

A production-grade, thread-safe, and invariant-enforcing Core Banking Engine implemented in pure Java Standard Edition (Java 21).

---

## 1. Project Architecture & Layered Package Structure

The system is organized into clean, decoupled layers adhering to the Single Responsibility Principle and Interface Segregation:

```
src/
└── com/
    └── bank/
        ├── model/
        │   ├── Customer.java                      # Customer aggregate root with immutable identity
        │   ├── BankAccount.java                   # Abstract base account implementing Transactable & Reportable
        │   ├── SavingsAccount.java                # Enforces $1,000 minimum balance invariant & interest calculation
        │   ├── CurrentAccount.java                # Enforces overdraft facility and auto-penalty fee assessment
        │   ├── Transaction.java                   # Immutable record representing passbook line items
        │   └── TransactionType.java               # Enum for DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN, INTEREST, FEE
        ├── service/
        │   ├── Transactable.java                  # Monetary transaction interface with method overloading
        │   ├── Reportable.java                    # Ledger auditing and bank statement reporting interface
        │   ├── BankService.java                   # Core domain service interface
        │   └── BankServiceImpl.java               # Thread-safe service with deterministic deadlock-free transfers
        ├── exception/
        │   ├── BankingException.java              # Base checked banking exception
        │   ├── InsufficientFundsException.java    # Detailed min balance breach metadata
        │   ├── OverdraftLimitExceededException.java # Detailed overdraft limit breach metadata
        │   ├── AccountNotFoundException.java      # Missing account lookup error
        │   ├── CustomerNotFoundException.java     # Missing customer lookup error
        │   ├── DuplicateAccountException.java     # Duplicate account registration error
        │   └── InvalidAmountException.java        # Negative or non-positive amount violation
        ├── concurrency/
        │   ├── ConcurrentTransactionWorker.java   # Worker task abstraction for thread coordination
        │   └── ConcurrencyHarness.java            # Multi-channel race condition & deadlock-free stress testing
        ├── ui/
        │   └── BankApplicationCLI.java            # Interactive console application with defensive input handling
        └── test/
            ├── BankingSystemTest.java             # Comprehensive automated unit and domain invariant test suite
            └── CLIIntegrationTest.java            # End-to-end interactive CLI session integration test
```

---

## 2. Compilation and Execution Guide

### Prerequisites
- **Java Development Kit (JDK)**: Version 17 or Version 21+ (Java 21 LTS recommended).
- **Operating System**: Windows, Linux, or macOS.

### Step 1: Compile All Source Files
From the project root directory:

**Windows (PowerShell):**
```powershell
if (!(Test-Path bin)) { New-Item -ItemType Directory -Path bin }
javac -d bin (Get-ChildItem -Path src -Recurse -Filter *.java).FullName
```

**Linux / macOS:**
```bash
mkdir -p bin
javac -d bin $(find src -name "*.java")
```

### Step 2: Run the Interactive CLI Application
```powershell
java -cp bin com.bank.ui.BankApplicationCLI
```

### Step 3: Run the Concurrency & Multithreading Stress Harness
```powershell
java -cp bin com.bank.concurrency.ConcurrencyHarness
```

### Step 4: Run the Invariant Unit Test Suite
```powershell
java -cp bin com.bank.test.BankingSystemTest
```

---

## 3. Core OOP Principles & Implementation Rationale

### A. Encapsulation & Defensive Copying
1. **Audit Ledger Protection**:
   - `BankAccount` maintains an internal `List<Transaction>`. To ensure ledger immutability and prevent external tampering, `getTransactionHistory()` returns `Collections.unmodifiableList(new ArrayList<>(transactions))` synchronized on the account's internal lock.
2. **Customer Associated Accounts**:
   - `Customer.getAssociatedAccountNumbers()` returns an unmodifiable list copy (`Collections.unmodifiableList(...)`), preventing unauthorized external additions or deletions without going through proper business methods.
3. **Immutable Transactions**:
   - `Transaction` is implemented as a modern Java `record`, rendering all transaction attributes final, non-null, and tamper-proof.

### B. Abstraction & Interface Segregation
- **`Transactable` Interface**: Defines the operational contracts for account deposits and withdrawals, employing **method overloading** (`deposit(double amount)` and `deposit(double amount, String note)`).
- **`Reportable` Interface**: Segregates read-only auditing and passbook statement generation from transactional mutation.
- **`BankAccount` Abstract Class**: Acts as the template base, implementing generic deposit validation and transaction history while forcing concrete subclasses (`SavingsAccount`, `CurrentAccount`) to supply distinct withdrawal rules.

### C. Polymorphism & Inheritance Invariants
- **`SavingsAccount`**:
  - Invariant: Account balance cannot drop below `MINIMUM_BALANCE` ($1,000.00).
  - Throws `InsufficientFundsException` containing the current balance, requested withdrawal, and minimum balance rule.
  - Implements `applyInterest()` to credit periodic accrued interest.
- **`CurrentAccount`**:
  - Invariant: Account can be drawn down to `-(overdraftLimit)`.
  - Throws `OverdraftLimitExceededException` if requested withdrawal exceeds available balance + overdraft limit.
  - Automatically assesses and records an `OVERDRAFT_FEE` ($50.00) whenever overdraft credit is tapped.

### D. Java Object Method Overrides (`equals`, `hashCode`, `toString`)
- **`Customer`**:
  - Identity equality (`equals()` and `hashCode()`) is strictly keyed to `customerId`.
- **`BankAccount`**:
  - Identity equality (`equals()` and `hashCode()`) is strictly keyed to `accountNumber`. Two account objects with the same account number represent the same ledger account regardless of memory reference.
- **`Transaction.toString()`**:
  - Produces clean tabular passbook ledger rows with aligned monetary figures and transaction types.

---

## 4. Concurrency Model & Deadlock Prevention Mechanism

### A. Fine-Grained Locking
Instead of synchronizing the entire banking service (which would create a massive concurrency bottleneck), each `BankAccount` maintains its own dedicated `lock` object (`private final Object lock = new Object();`). Balance updates, withdrawal rule checks, and transaction logging execute inside fine-grained synchronized blocks.

### B. The Deadlock Problem in Bidirectional Transfers
Consider two concurrent transfer operations:
- **Thread 1**: Transfers $500 from Account `ACC-A` to Account `ACC-B`.
- **Thread 2**: Transfers $500 from Account `ACC-B` to Account `ACC-A`.

If Thread 1 locks `ACC-A` and waits for `ACC-B`, while Thread 2 locks `ACC-B` and waits for `ACC-A`, a circular wait condition (**Deadlock**) occurs.

### C. The Solution: Deterministic Global Lock Ordering
In `BankServiceImpl.transfer(...)`, account locks are **always acquired in a strict, globally consistent lexicographical order** based on `accountNumber.compareTo(...)`:

```java
// Deterministic Lock Ordering
BankAccount firstLockAccount;
BankAccount secondLockAccount;

if (fromAccount.getAccountNumber().compareTo(toAccount.getAccountNumber()) < 0) {
    firstLockAccount = fromAccount;
    secondLockAccount = toAccount;
} else {
    firstLockAccount = toAccount;
    secondLockAccount = fromAccount;
}

// Nested deterministic acquisition eliminates circular wait
synchronized (firstLockAccount.getLock()) {
    synchronized (secondLockAccount.getLock()) {
        fromAccount.withdrawTransfer(amount, senderNarrative);
        toAccount.depositTransfer(amount, receiverNarrative);
    }
}
```

Because all threads in the JVM acquire `ACC-A` before `ACC-B`, circular wait is mathematically impossible, eliminating deadlocks.

---

## 5. Concurrency Harness Scenarios & Demonstration

The `ConcurrencyHarness` provides reproducible verification of the system under heavy concurrent load:

### Scenario 1: Multi-Channel Race Condition on a Shared Account
- **Setup**: One `SavingsAccount` with $10,000.00 balance ($1,000.00 minimum balance invariant).
- **Stress**: 10 simultaneous threads (ATM, POS, Web, Mobile) each attempting to withdraw $1,200.00 (Total $12,000.00 requested).
- **Result**:
  - Exactly **7 successful debits** (7 × $1,200 = $8,400.00 deducted).
  - Exactly **3 rejected debits** with `InsufficientFundsException`.
  - Final ledger balance is preserved at **$1,600.00** (above the $1,000.00 minimum).
  - Passbook contains exactly 8 audited line items (1 opening deposit + 7 withdrawals).

### Scenario 2: High-Frequency Bidirectional Transfers (Deadlock-Free)
- **Setup**: Account 1 ($20,000.00) and Account 2 ($20,000.00). Total pooled wealth = $40,000.00.
- **Stress**:
  - Thread A performs 500 transfers of $100 from Account 1 $\rightarrow$ Account 2.
  - Thread B performs 500 transfers of $100 from Account 2 $\rightarrow$ Account 1.
- **Result**:
  - All **1,000 transfers execute in parallel without deadlock**.
  - Final balances remain exactly **$20,000.00** each.
  - Conservation of money holds: Total pooled money = **$40,000.00**.

---

## 6. Interactive CLI Overview

The interactive CLI (`BankApplicationCLI`) provides an intuitive, menu-driven interface with defensive parsing:
1. **Register New Customer**: Validates name, email, and phone before generating a unique customer ID.
2. **Open Savings Account**: Configures interest rate and enforces the $1,000 minimum balance rule.
3. **Open Current Account**: Configures approved overdraft limits and registers automatic penalty fee logic.
4. **Deposit Funds**: Supports standard cash deposits and deposits with custom audit memos.
5. **Withdraw Funds**: Executes polymorphic debit rules with custom error messages on minimum balance or overdraft breaches.
6. **Inter-Account Transfer**: Executes atomic, deadlock-free transfers between any two accounts.
7. **View Account Statement**: Prints a formal, formatted passbook ledger with timestamps, transaction types, amounts, and post-transaction balances.
8. **View Customer Profile**: Summarizes total relationship value and lists all owned accounts.
9. **Run Multithreading Stress Test**: Triggers the `ConcurrencyHarness` test suite directly from the CLI.
10. **Exit**: Gracefully closes resources.
