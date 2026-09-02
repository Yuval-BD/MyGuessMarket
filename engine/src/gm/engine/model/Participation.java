package gm.engine.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One user's involvement in one event: what they hold, what they have paid and received, and how
 * much commission they have paid along the way.
 */

public class Participation {

    private final User user;
    private final Map<String, Holding> holdings = new LinkedHashMap<>();

    private double totalSpent;
    private double totalReceived;
    private double totalCommissionPaid;

    public Participation(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public String getUserName() {
        return user.getName();
    }

    public void recordPurchase(String optionName, long quantity, double sharesCost, double commission) {
        getOrCreateHolding(optionName).add(quantity, sharesCost);
        totalSpent += sharesCost + commission;
        totalCommissionPaid += commission;
    }

    public void recordSale(String optionName, long quantity, double proceeds, double commission) {
        getOrCreateHolding(optionName).remove(quantity);
        totalReceived += proceeds - commission;
        totalCommissionPaid += commission;
    }

    public void recordPayout(double gross, double commission) {
        totalReceived += gross - commission;
        totalCommissionPaid += commission;
    }

    public long getShares(String optionName) {
        Holding holding = holdings.get(optionName);
        return holding == null ? 0 : holding.getShares();
    }

    public double getAmountPaid(String optionName) {
        Holding holding = holdings.get(optionName);
        return holding == null ? 0 : holding.getAmountPaid();
    }

    public Collection<Holding> getHoldings() {
        return Collections.unmodifiableCollection(holdings.values());
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public double getTotalReceived() {
        return totalReceived;
    }

    public double getTotalCommissionPaid() {
        return totalCommissionPaid;
    }

    public double getNetResult() {
        return totalReceived - totalSpent;
    }

    private Holding getOrCreateHolding(String optionName) {
        return holdings.computeIfAbsent(optionName, Holding::new);
    }
}
