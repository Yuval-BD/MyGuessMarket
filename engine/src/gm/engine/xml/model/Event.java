package gm.engine.xml.model;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import gm.engine.exception.*;
import gm.engine.market.MarketMaker;

public class Event {
    private final int id;
    private final int eventNumber;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final List<EventOption> options;
    private final MarketMaker marketMaker;
    private final Account account;
    private final List<Trade> trades = new ArrayList<>();
    private EventStatus status = EventStatus.ACTIVE;
    private EventOption winningOption;

    public Event(int eventNumber, int id, String name, String description,
                 int commissionPercent, CommissionType commissionType,
                 List<EventOption> options, MarketMaker marketMaker) {

        if (options == null || options.size() != 2) {
            throw new InvalidEventDataException(
                    String.format("Error: Event \"%s\" - must have exactly 2 options, but found %d.",
                            name, options == null ? 0 : options.size()));
        }
        if (commissionPercent < 0 || commissionPercent > 90) {
            throw new InvalidEventDataException(
                    String.format("Error: Event \"%s\" - Commission must be between 0 and 90, but found %d",
                            name, commissionPercent));
        }

        this.eventNumber = eventNumber;
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.options = List.copyOf(options);
        this.marketMaker = marketMaker;
        this.account = new Account();
        this.account.deposit(marketMaker.initialSubsidy(options.size()));
    }

    public void recordTrade(Trade trade) {
        if (status != EventStatus.ACTIVE) {
            throw new EventNotActiveException(
                    String.format("Error: Event \"%s\" - event is closed, cannot trade in a closed event.", name));
        }
        trades.add(trade);
    }

    public void close(EventOption winner) {
        if (status != EventStatus.ACTIVE) {
            throw new EventNotActiveException(
                    String.format("Error: Event \"%s\" - event is already closed, cannot close a closed event.",
                            name));
        }
        if (!options.contains(winner)) {
            throw new OptionNotFoundException(
                    String.format("Error: Event \"%s\" - given winning option (%s) is not one of the event options.",
                            name, winner.getName()));
        }
        winningOption = winner;
        status = EventStatus.CLOSED;
    }

    public EventOption getOption(int optionNumber) {
        if (optionNumber < 1 || optionNumber > options.size()) {
            throw new OptionNotFoundException(
                    String.format("Error: Event \"%s\" - option number %d does not exist.",
                            name,  optionNumber));
        }
        return options.get(optionNumber - 1);
    }

    public int getEventNumber() { return eventNumber; }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommissionPercent() { return commissionPercent; }
    public CommissionType getCommissionType() { return commissionType; }
    public List<EventOption> getOptions() { return options; }
    public MarketMaker getMarketMaker() { return marketMaker; }
    public Account getAccount() { return account; }
    public List<Trade> getTrades() { return Collections.unmodifiableList(trades); }
    public EventStatus getStatus() { return status; }
    public boolean isActive() { return status == EventStatus.ACTIVE; }
    public EventOption getWinningOption() { return winningOption; }
    public long[] getSharesArray() {
        long[] shares = new long[options.size()];
        for (int i = 0; i < options.size(); i++) {
            shares[i] = options.get(i).getSharesBought();
        }
        return shares;
    }
}
