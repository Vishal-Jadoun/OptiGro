package com.optigro.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class Customer {
    private final int customerId;
    private final int userId;
    private final String name;
    private final String phone;
    private final String address;
    private double walletBalance;
    private final Set<String> usedCoupons;

    public Customer(int customerId, int userId, String name, String phone,
                    String address, double walletBalance) {
        if (walletBalance < 0) {
            throw new IllegalArgumentException("Wallet balance cannot be negative");
        }
        this.customerId = customerId;
        this.userId = userId;
        this.name = Objects.requireNonNull(name, "Name required");
        this.phone = Objects.requireNonNull(phone, "Phone required");
        this.address = Objects.requireNonNull(address, "Address required");
        this.walletBalance = walletBalance;
        this.usedCoupons = new HashSet<>();
    }

    public int getCustomerId() { return customerId; }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public double getWalletBalance() { return walletBalance; }

    public void setWalletBalance(double walletBalance) {
        if (walletBalance < 0) {
            throw new IllegalArgumentException("Wallet balance cannot be negative");
        }
        this.walletBalance = walletBalance;
    }

    public boolean deductWallet(double amount) {
        if (amount <= 0 || walletBalance < amount) {
            return false;
        }
        walletBalance -= amount;
        return true;
    }

    public void addWallet(double amount) {
        if (amount > 0) {
            walletBalance += amount;
        }
    }

    // Set / HashSet demonstration for unique single-use coupon tracking
    public boolean addUsedCoupon(String couponCode) {
        if (couponCode == null) return false;
        return usedCoupons.add(couponCode.trim().toUpperCase());
    }

    public boolean hasUsedCoupon(String couponCode) {
        if (couponCode == null) return false;
        return usedCoupons.contains(couponCode.trim().toUpperCase());
    }

    public Set<String> getUsedCoupons() {
        return Collections.unmodifiableSet(usedCoupons);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return customerId == customer.customerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return String.format("Customer #%d: %s | Phone: %s | Wallet: Rs.%.2f",
            customerId, name, phone, walletBalance);
    }
}
