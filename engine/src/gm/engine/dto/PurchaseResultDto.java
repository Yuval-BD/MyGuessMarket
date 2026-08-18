package gm.engine.dto;

public final class PurchaseResultDto {

    private final double sharesCost;
    private final double commission;
    private final double totalPaid;
    private final EventStateDto stateAfter;

    public PurchaseResultDto(double sharesCost, double commission, double totalPaid, EventStateDto stateAfter) {
        this.sharesCost = sharesCost;
        this.commission = commission;
        this.totalPaid = totalPaid;
        this.stateAfter = stateAfter;
    }

    public double getSharesCost() { return sharesCost; }
    public double getCommission() { return commission; }
    public double getTotalPaid() { return totalPaid; }
    public EventStateDto getStateAfter() { return stateAfter; }
}