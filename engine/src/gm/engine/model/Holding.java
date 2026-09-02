package gm.engine.model;

import gm.engine.exception.InvalidQuantityException;

public class Holding {

    private final String optionName;
    private long shares = 0;
    private double amountPaid = 0;

    public Holding(String optionName) {
        this.optionName = optionName;
    }

    public String getOptionName() {
        return optionName;
    }

    public long getShares() {
        return shares;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void add(long quantity, double cost) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(String.format(
                    "Error: quantity must be positive, but got %d.", quantity));
        }
        if (cost < 0) {
            throw new InvalidQuantityException(String.format(
                    "Error: cost cannot be negative, but got %.2f.", cost));
        }
        shares += quantity;
        amountPaid += cost;
    }

    public void remove(long quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(String.format(
                    "Error: quantity must be positive, but got %d.", quantity));
        }
        if (quantity > shares) {
            throw new InvalidQuantityException(String.format(
                    "Error: cannot remove %d shares of \"%s\" - only %d are held.",
                    quantity, optionName, shares));
        }

        double averageCost = amountPaid / shares;
        shares -= quantity;
        amountPaid -= averageCost * quantity;

        if (shares == 0) {
            amountPaid = 0;
        }
    }
}
