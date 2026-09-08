package gm.engine.dto;

public final class CloseResultDto {

    private final String winningOptionName;
    private final double totalPaidToWinners;
    private final double totalCommissionCollected;
    private final double leftoverReturnedToMarketMaker;

    public CloseResultDto(String winningOptionName, double totalPaidToWinners,
                          double totalCommissionCollected, double leftoverReturnedToMarketMaker) {
        this.winningOptionName = winningOptionName;
        this.totalPaidToWinners = totalPaidToWinners;
        this.totalCommissionCollected = totalCommissionCollected;
        this.leftoverReturnedToMarketMaker = leftoverReturnedToMarketMaker;
    }

    public String getWinningOptionName() { return winningOptionName; }
    public double getTotalPaidToWinners() { return totalPaidToWinners; }
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public double getLeftoverReturnedToMarketMaker() { return leftoverReturnedToMarketMaker; }
}
