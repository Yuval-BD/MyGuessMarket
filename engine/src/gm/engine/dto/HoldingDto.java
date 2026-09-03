package gm.engine.dto;

public final class HoldingDto {

    private final String optionName;
    private final long shares;
    private final double amountPaid;

    public HoldingDto(String optionName, long shares, double amountPaid) {
        this.optionName = optionName;
        this.shares = shares;
        this.amountPaid = amountPaid;
    }

    public String getOptionName() { return optionName; }
    public long getShares() { return shares; }

    /** What was paid for the shares still held. */
    public double getAmountPaid() { return amountPaid; }
}
