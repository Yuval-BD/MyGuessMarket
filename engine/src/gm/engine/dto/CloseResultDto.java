package gm.engine.dto;

public final class CloseResultDto {

    private final String winningOptionName;
    private final double totalPaidToWinners;
    private final double totalCommissionCollected;
    private final double leftoverReturnedToMarketMaker;
    private final EventStateDto finalState;

    public CloseResultDto(String winningOptionName, double totalPaidToWinners,
                          double totalCommissionCollected, double leftoverReturnedToMarketMaker,
                          EventStateDto finalState) {
        this.winningOptionName = winningOptionName;
        this.totalPaidToWinners = totalPaidToWinners;
        this.totalCommissionCollected = totalCommissionCollected;
        this.leftoverReturnedToMarketMaker = leftoverReturnedToMarketMaker;
        this.finalState = finalState;
    }

    public String getWinningOptionName() { return winningOptionName; }
    public double getTotalPaidToWinners() { return totalPaidToWinners; }

    /** On-close commission moved to the market maker. Zero for on-purchase events. */
    public double getTotalCommissionCollected() { return totalCommissionCollected; }

    /** Unspent subsidy returned to the market maker. LMSR only, otherwise zero. */
    public double getLeftoverReturnedToMarketMaker() { return leftoverReturnedToMarketMaker; }

    /** The LMSR state after closing, or null for an order-book event, which has no price curve. */
    public EventStateDto getFinalState() { return finalState; }
}
