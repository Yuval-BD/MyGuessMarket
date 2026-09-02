package gm.engine.dto;

public final class TradeDto {

    private final String buyerName;
    private final String optionName;
    private final long quantity;
    private final double sharesCost;
    private final double commissionPaid;
    private final double totalPaid;

    public TradeDto(String buyerName, String optionName, long quantity,
                    double sharesCost, double commissionPaid, double totalPaid) {
        this.buyerName = buyerName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.sharesCost = sharesCost;
        this.commissionPaid = commissionPaid;
        this.totalPaid = totalPaid;
    }

    public String getBuyerName() { return buyerName; }
    public String getOptionName() { return optionName; }
    public long getQuantity() { return quantity; }
    public double getSharesCost() { return sharesCost; }
    public double getCommissionPaid() { return commissionPaid; }
    public double getTotalPaid() { return totalPaid; }
}
