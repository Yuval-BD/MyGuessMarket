package gm.engine.model;

import gm.engine.exception.InvalidEventDataException;
import gm.engine.exception.InvalidQuantityException;

public class Trade {

    private final String buyerName;
    private final EventOption option;
    private final long quantity;
    private final double sharesCost;
    private final double commissionPaid;
    private final double totalPaid;

    public Trade(String buyerName, EventOption option, long quantity,
                 double sharesCost, double commissionPaid) {
        if (buyerName == null || buyerName.isBlank()) {
            throw new InvalidEventDataException("Error: a trade must record who made it.");
        }
        if (option == null) {
            throw new InvalidEventDataException("Error: a trade must reference a valid option.");
        }
        if (quantity <= 0) {
            throw new InvalidQuantityException(
                    String.format("Error: trade quantity must be positive, but got %d.", quantity));
        }
        if (sharesCost < 0 || commissionPaid < 0) {
            throw new InvalidEventDataException("Error: trade cost and commission cannot be negative.");
        }

        this.buyerName = buyerName;
        this.option = option;
        this.quantity = quantity;
        this.sharesCost = sharesCost;
        this.commissionPaid = commissionPaid;
        this.totalPaid = calcTotalPaid(sharesCost, commissionPaid);
    }

    public String getBuyerName() {
        return buyerName;
    }

    public EventOption getOption() {
        return option;
    }

    public long getQuantity() {
        return quantity;
    }

    public double getSharesCost() {
        return sharesCost;
    }

    public double getCommissionPaid() {
        return commissionPaid;
    }

    private static double calcTotalPaid(double sharesCost, double commissionPaid) {
        return sharesCost + commissionPaid;
    }

    public double getTotalPaid() {
        return totalPaid;
    }
}