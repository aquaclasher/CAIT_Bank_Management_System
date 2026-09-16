package com.bank.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a retail bank customer.
 * Invariant: Identity and equality are strictly determined by customerId.
 */
public class Customer {
    private final String customerId;
    private String fullName;
    private String email;
    private String phone;
    private final List<String> associatedAccountNumbers;

    /**
     * Constructs a new Customer instance.
     *
     * @param customerId Unique customer identifier (e.g., CUST-1001)
     * @param fullName   Full name of the customer
     * @param email      Contact email address
     * @param phone      Contact phone number
     */
    public Customer(String customerId, String fullName, String email, String phone) {
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");
        this.phone = Objects.requireNonNull(phone, "phone cannot be null");
        this.associatedAccountNumbers = new ArrayList<>();
    }

    /**
     * Associates a newly opened bank account with this customer.
     *
     * @param accountNumber The unique account number to link
     */
    public synchronized void addAccount(String accountNumber) {
        Objects.requireNonNull(accountNumber, "accountNumber cannot be null");
        if (!associatedAccountNumbers.contains(accountNumber)) {
            associatedAccountNumbers.add(accountNumber);
        }
    }

    /**
     * Returns an unmodifiable defensive copy of the customer's associated account numbers.
     *
     * @return Unmodifiable list of account numbers
     */
    public synchronized List<String> getAssociatedAccountNumbers() {
        return Collections.unmodifiableList(new ArrayList<>(associatedAccountNumbers));
    }

    public String getCustomerId() {
        return customerId;
    }

    public synchronized String getFullName() {
        return fullName;
    }

    public synchronized void setFullName(String fullName) {
        this.fullName = Objects.requireNonNull(fullName, "fullName cannot be null");
    }

    public synchronized String getEmail() {
        return email;
    }

    public synchronized void setEmail(String email) {
        this.email = Objects.requireNonNull(email, "email cannot be null");
    }

    public synchronized String getPhone() {
        return phone;
    }

    public synchronized void setPhone(String phone) {
        this.phone = Objects.requireNonNull(phone, "phone cannot be null");
    }

    /**
     * Strict identity equality based on customerId.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    /**
     * Hash code based strictly on customerId.
     */
    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public synchronized String toString() {
        return String.format("Customer[ID: %s | Name: %s | Email: %s | Phone: %s | Accounts: %s]",
                customerId, fullName, email, phone, associatedAccountNumbers);
    }
}
