package gm.engine.dto;

public final class PurchaseResultDto {

    private final String buyerName;
    private final String optionName;
    private final long quantity;
    private final double sharesCost;
    private final double commissionPaid;
    private final double totalPaid;
    private final double buyerBalanceAfter;
    private final EventStateDto stateAfter;

    public PurchaseResultDto(String buyerName, String optionName, long quantity,
                             double sharesCost, double commissionPaid, double totalPaid,
                             double buyerBalanceAfter, EventStateDto stateAfter) {
        this.buyerName = buyerName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.sharesCost = sharesCost;
        this.commissionPaid = commissionPaid;
        this.totalPaid = totalPaid;
        this.buyerBalanceAfter = buyerBalanceAfter;
        this.stateAfter = stateAfter;
    }

    public String getBuyerName() { return buyerName; }
    public String getOptionName() { return optionName; }
    public long getQuantity() { return quantity; }
    public double getSharesCost() { return sharesCost; }
    public double getCommissionPaid() { return commissionPaid; }
    public double getTotalPaid() { return totalPaid; }
    public double getBuyerBalanceAfter() { return buyerBalanceAfter; }
    public EventStateDto getStateAfter() { return stateAfter; }
}
