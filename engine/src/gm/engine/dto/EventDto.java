package gm.engine.dto;

import java.util.List;

/**
 * Summary of one event, as shown in the events table. Enough to display and filter a row without
 * asking the engine anything further.
 */
public final class EventDto {

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionTypeDto commissionType;
    private final TradingMethodTypeDto methodType;
    private final EventStatusDto status;
    private final List<String> optionNames;
    private final String marketMakerName;
    private final double accountBalance;
    private final String winningOptionName;

    public EventDto(int id, String name, String description,
                    int commissionPercent, CommissionTypeDto commissionType,
                    TradingMethodTypeDto methodType, EventStatusDto status,
                    List<String> optionNames, String marketMakerName,
                    double accountBalance, String winningOptionName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.methodType = methodType;
        this.status = status;
        this.optionNames = List.copyOf(optionNames);
        this.marketMakerName = marketMakerName;
        this.accountBalance = accountBalance;
        this.winningOptionName = winningOptionName;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommissionPercent() { return commissionPercent; }
    public CommissionTypeDto getCommissionType() { return commissionType; }
    public TradingMethodTypeDto getMethodType() { return methodType; }
    public EventStatusDto getStatus() { return status; }
    public List<String> getOptionNames() { return optionNames; }
    public String getMarketMakerName() { return marketMakerName; }
    public double getAccountBalance() { return accountBalance; }

    /** Null while the event is still open. */
    public String getWinningOptionName() { return winningOptionName; }
}
