package gm.engine.dto;

import java.util.List;

public final class EventStateDto {

    private final int eventNumber;
    private final int id;
    private final String name;
    private final boolean active;
    private final List<OptionStateDto> options;
    private final double accountBalance;
    private final double totalCommissionCollected;
    private final List<TradeDto> trades;
    private final String winningOptionName;
    private final Double totalPaidToWinners;

    public EventStateDto(int eventNumber, int id, String name, boolean active,
                         List<OptionStateDto> options, double accountBalance,
                         double totalCommissionCollected, List<TradeDto> trades,
                         String winningOptionName, Double totalPaidToWinners) {
        this.eventNumber = eventNumber;
        this.id = id;
        this.name = name;
        this.active = active;
        this.options = List.copyOf(options);
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.trades = List.copyOf(trades);
        this.winningOptionName = winningOptionName;
        this.totalPaidToWinners = totalPaidToWinners;
    }

    public int getEventNumber() { return eventNumber; }
    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public List<OptionStateDto> getOptions() { return options; }
    public double getAccountBalance() { return accountBalance; }
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public List<TradeDto> getTrades() { return trades; }
    public String getWinningOptionName() { return winningOptionName; }
    public Double getTotalPaidToWinners() { return totalPaidToWinners; }
}