package gm.engine.dto;

public final class CloseResultDto {

    private final String winningOptionName;
    private final double commissionCharged;
    private final double totalPaidToWinners;
    private final EventStateDto finalState;

    public CloseResultDto(String winningOptionName, double commissionCharged,
                          double totalPaidToWinners, EventStateDto finalState) {
        this.winningOptionName = winningOptionName;
        this.commissionCharged = commissionCharged;
        this.totalPaidToWinners = totalPaidToWinners;
        this.finalState = finalState;
    }

    public String getWinningOptionName() { return winningOptionName; }
    public double getCommissionCharged() { return commissionCharged; }
    public double getTotalPaidToWinners() { return totalPaidToWinners; }
    public EventStateDto getFinalState() { return finalState; }
}