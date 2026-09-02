package gm.engine.dto;

import java.util.List;

/**
 * The trading state of one LMSR event: current prices, the event's own account, and its trade
 * history newest-first.
 * <p>
 * Order-book events have a different shape entirely - two books, resting orders, bid/ask/mid/spread
 * - and get their own DTO in a later phase rather than being forced into this one.
 */
public final class EventStateDto {

    private final int id;
    private final String name;
    private final EventStatusDto status;
    private final List<OptionStateDto> optionStates;
    private final double accountBalance;
    private final double commissionCollected;
    private final String marketMakerName;
    private final List<TradeDto> trades;
    private final String winningOptionName;

    public EventStateDto(int id, String name, EventStatusDto status,
                         List<OptionStateDto> optionStates, double accountBalance,
                         double commissionCollected, String marketMakerName,
                         List<TradeDto> trades, String winningOptionName) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.optionStates = List.copyOf(optionStates);
        this.accountBalance = accountBalance;
        this.commissionCollected = commissionCollected;
        this.marketMakerName = marketMakerName;
        this.trades = List.copyOf(trades);
        this.winningOptionName = winningOptionName;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public EventStatusDto getStatus() { return status; }
    public List<OptionStateDto> getOptionStates() { return optionStates; }
    public double getAccountBalance() { return accountBalance; }

    /** Commission this event has generated for its market maker so far. */
    public double getCommissionCollected() { return commissionCollected; }

    public String getMarketMakerName() { return marketMakerName; }

    /** Newest first, so the UI does not have to reverse it. */
    public List<TradeDto> getTrades() { return trades; }

    /** Null while the event is still open. */
    public String getWinningOptionName() { return winningOptionName; }
}
