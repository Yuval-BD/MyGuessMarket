package gm.engine.dto;

import java.util.List;

/**
 * The trading state of one LMSR event: current prices, the event's own account, and its trade
 * history newest-first.
 * <p>
 * Order-book events have a different shape entirely - two books, resting orders, bid/ask/mid/spread
 * - so they get their own {@link OrderBookStateDto} rather than being forced into this one. That is
 * why this type is named for LMSR: asking for it on an order-book event is an error, not an empty
 * result.
 */
public final class LmsrStateDto {

    private final List<OptionStateDto> optionStates;
    private final double accountBalance;
    private final double commissionCollected;
    private final List<TradeDto> trades;
    private final String winningOptionName;

    public LmsrStateDto(List<OptionStateDto> optionStates, double accountBalance,
                        double commissionCollected, List<TradeDto> trades,
                        String winningOptionName) {
        this.optionStates = optionStates;
        this.accountBalance = accountBalance;
        this.commissionCollected = commissionCollected;
        this.trades = trades;
        this.winningOptionName = winningOptionName;
    }

    public List<OptionStateDto> getOptionStates() { return optionStates; }
    public double getAccountBalance() { return accountBalance; }
    public double getCommissionCollected() { return commissionCollected; }
    public List<TradeDto> getTrades() { return trades; }
    public String getWinningOptionName() { return winningOptionName; }
}
