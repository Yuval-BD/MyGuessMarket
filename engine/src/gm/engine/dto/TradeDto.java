package gm.engine.dto;

public final class TradeDto {

    private final String optionName;
    private final long quantity;
    private final double totalPaid;

    public TradeDto(String optionName, long quantity, double totalPaid) {
        this.optionName = optionName;
        this.quantity = quantity;
        this.totalPaid = totalPaid;
    }

    public String getOptionName() { return optionName; }
    public long getQuantity() { return quantity; }
    public double getTotalPaid() { return totalPaid; }
}