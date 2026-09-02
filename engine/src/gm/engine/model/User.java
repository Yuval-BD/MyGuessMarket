package gm.engine.model;

import gm.engine.exception.InsufficientFundsException;
import gm.engine.exception.InvalidUserDataException;
import gm.engine.exception.UserBlockedException;

public class User {

    private final String name;
    private final Account account;
    private boolean blocked;

    public User(String name, int initialCash) {
        if (name == null || name.isBlank()) {
            throw new InvalidUserDataException("Error: a user name must not be blank.");
        }
        if (initialCash <= 0) {
            throw new InvalidUserDataException(String.format(
                    "Error: user \"%s\" - initial cash must be greater than 0, but found %d.",
                    name.trim(), initialCash));
        }

        this.name = name.trim();
        this.account = new Account();
        this.account.deposit(initialCash);
    }

    public String getName() {
        return name;
    }

    public Account getAccount() {
        return account;
    }

    public double getBalance() {
        return account.getBalance();
    }

    public double getTotalCommissionCollected() {
        return account.getTotalCommissionCollected();
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean canAfford(double amount) {
        return account.getBalance() >= amount;
    }

    public void charge(double amount) {
        if (!canAfford(amount)) {
            throw new InsufficientFundsException(String.format(
                    "Error: %s needs $%.2f but has only $%.2f available.", name, amount, getBalance()));
        }
        account.withdraw(amount);
    }

    public boolean chargeAllowingOverdraft(double amount) {
        account.withdraw(amount);
        if (!blocked && account.isOverdrawn()) {
            blocked = true;
            return true;
        }
        return false;
    }

    public void credit(double amount) {
        account.deposit(amount);
    }

    public void collectCommission(double amount) {
        account.depositCommission(amount);
    }

    public void requireNotBlocked() {
        if (blocked) {
            throw new UserBlockedException(String.format(
                    "Error: %s went into a negative balance and is blocked from acting in the system.",
                    name));
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
