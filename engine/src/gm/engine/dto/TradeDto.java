package gm.engine.dto;

public final class TradeDto {

    private final String optionName;
    private final long quantity;
    private final double totalPaid;

    public TradeDto(String optionName, long quantity, double sharesCost, double commissionPaid, double totalPaid) {
        this.optionName = optionName;
        this.quantity = quantity;
        this.sharesCost = sharesCost;
        this.commissionPaid = commissionPaid;
        this.totalPaid = totalPaid;
    }

    public String getOptionName() { return optionName; }
    public long getQuantity() { return quantity; }
    public double getTotalPaid() { return totalPaid; }
}