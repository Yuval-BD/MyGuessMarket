package gm.engine.xml.model;

import gm.engine.exception.InvalidQuantityException;

public class Account {

    private double balance = 0;
    private double totalCommissionCollected = 0;

    public double getBalance() {
        return balance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public void deposit(double amount) {
        if (amount < 0) {
            throw new InvalidQuantityException(
                    String.format("Error: cannot deposit a negative amount (%.2f).", amount));
        }
        balance += amount;
    }

    public void depositCommission(double amount) {
        deposit(amount);
        totalCommissionCollected += amount;
    }

    public void withdraw(double amount) {
        if (amount < 0) {
            throw new InvalidQuantityException(
                    String.format("Error: cannot withdraw a negative amount (%.2f).", amount));
        }
        balance -= amount;
    }
}