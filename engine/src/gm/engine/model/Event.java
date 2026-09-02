package gm.engine.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gm.engine.exception.EventNotActiveException;
import gm.engine.exception.InvalidEventDataException;
import gm.engine.exception.InvalidEventStateException;
import gm.engine.exception.NotMarketMakerException;
import gm.engine.exception.OptionNotFoundException;
import gm.engine.trading.LmsrTradingMethod;
import gm.engine.trading.OrderBookTradingMethod;
import gm.engine.trading.TradingMethod;

public class Event {

    private static final double PERCENT = 100.0;
    private static final double LMSR_PAYOUT_PER_SHARE = 1.0;

    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionType commissionType;
    private final List<EventOption> options;
    private final TradingMethod tradingMethod;
    private final User marketMaker;

    private final Account account = new Account();
    private final Map<String, Participation> participants = new LinkedHashMap<>();
    private final List<Trade> trades = new ArrayList<>();

    private double commissionCollected = 0;
    private EventStatus status = EventStatus.NOT_STARTED;
    private EventOption winningOption;

    public Event(int id, String name, String description,
                 int commissionPercent, CommissionType commissionType,
                 List<EventOption> options, TradingMethod tradingMethod, User marketMaker) {

        if (options == null || options.size() != 2) {
            throw new InvalidEventDataException(String.format(
                    "Error: Event \"%s\" - must have exactly 2 options, but found %d.",
                    name, options == null ? 0 : options.size()));
        }
        if (commissionPercent < 0 || commissionPercent > 90) {
            throw new InvalidEventDataException(String.format(
                    "Error: Event \"%s\" - commission must be between 0 and 90, but found %d.",
                    name, commissionPercent));
        }
        if (marketMaker == null) {
            throw new InvalidEventDataException(String.format(
                    "Error: Event \"%s\" - every event must have a market maker.", name));
        }

        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.options = List.copyOf(options);
        this.tradingMethod = tradingMethod;
        this.marketMaker = marketMaker;
    }

    public void open(User actor) {
        if (status != EventStatus.NOT_STARTED) {
            throw new InvalidEventStateException(String.format(
                    "Error: event \"%s\" cannot be opened because it is %s.",
                    name, status == EventStatus.ACTIVE ? "already active" : "already closed"));
        }
        if (actor != marketMaker) {
            throw new NotMarketMakerException(String.format(
                    "Error: only %s can open the event \"%s\".", marketMaker.getName(), name));
        }
        actor.requireNotBlocked();

        double openingCost = tradingMethod.openingInvestment(options.size());
        actor.charge(openingCost);
        account.deposit(openingCost);

        if (tradingMethod instanceof OrderBookTradingMethod orderBook) {
            allocateInitialShares(actor, orderBook, openingCost);
        }

        status = EventStatus.ACTIVE;
    }

    public ClosingOutcome close(User actor, EventOption winner) {
        if (status != EventStatus.ACTIVE) {
            throw new InvalidEventStateException(String.format(
                    "Error: event \"%s\" cannot be closed because it %s.",
                    name, status == EventStatus.NOT_STARTED ? "has not started yet" : "is already closed"));
        }
        if (actor != marketMaker) {
            throw new NotMarketMakerException(String.format(
                    "Error: only %s can close the event \"%s\".", marketMaker.getName(), name));
        }
        if (winner == null || !options.contains(winner)) {
            throw new OptionNotFoundException(String.format(
                    "Error: event \"%s\" - \"%s\" is not one of this event's options.",
                    name, winner == null ? "" : winner.getName()));
        }

        double payoutPerShare = payoutPerShare();
        double totalPaidToWinners = 0;
        double totalCommission = 0;

        for (Participation participation : participants.values()) {
            long winningShares = participation.getShares(winner.getName());
            if (winningShares <= 0) {
                continue;
            }
            double gross = winningShares * payoutPerShare;
            double commission = commissionType == CommissionType.ON_CLOSE
                    ? gross * commissionPercent / PERCENT
                    : 0;

            account.withdraw(gross);
            participation.getUser().credit(gross - commission);
            marketMaker.collectCommission(commission);
            commissionCollected += commission;
            participation.recordPayout(gross, commission);

            totalPaidToWinners += gross;
            totalCommission += commission;
        }

        double leftover = returnLeftoverToMarketMaker();

        winningOption = winner;
        status = EventStatus.CLOSED;

        return new ClosingOutcome(winner.getName(), totalPaidToWinners, totalCommission, leftover);
    }

    public Trade buyLmsr(User buyer, int optionNumber, long quantity) {
        if (status != EventStatus.ACTIVE) {
            throw new EventNotActiveException(String.format(
                    "Error: event \"%s\" is %s, so it cannot be traded in.",
                    name, status == EventStatus.NOT_STARTED ? "not open yet" : "closed"));
        }
        if (!(tradingMethod instanceof LmsrTradingMethod lmsr)) {
            throw new InvalidEventStateException(String.format(
                    "Error: event \"%s\" is an order book event - submit an order to trade in it.",
                    name));
        }
        buyer.requireNotBlocked();

        EventOption option = getOption(optionNumber);
        double sharesCost = lmsr.costOfBuying(optionNumber - 1, quantity, getSharesArray());
        double commission = commissionType == CommissionType.ON_PURCHASE
                ? sharesCost * commissionPercent / PERCENT
                : 0;

        buyer.charge(sharesCost + commission);
        account.deposit(sharesCost);
        marketMaker.collectCommission(commission);
        commissionCollected += commission;
        option.addShares(quantity);

        Trade trade = new Trade(buyer.getName(), option, quantity, sharesCost, commission);
        participationOf(buyer).recordPurchase(option.getName(), quantity, sharesCost, commission);
        trades.add(trade);
        return trade;
    }

    public Participation participationOf(User user) {
        return participants.computeIfAbsent(user.getName(), key -> new Participation(user));
    }

    public Participation getParticipation(String userName) {
        return participants.get(userName);
    }

    public Collection<Participation> getParticipants() {
        return Collections.unmodifiableCollection(participants.values());
    }

    public EventOption getOption(int optionNumber) {
        if (optionNumber < 1 || optionNumber > options.size()) {
            throw new OptionNotFoundException(String.format(
                    "Error: event \"%s\" - option number %d does not exist. Valid range is 1 to %d.",
                    name, optionNumber, options.size()));
        }
        return options.get(optionNumber - 1);
    }

    public long[] getSharesArray() {
        long[] shares = new long[options.size()];
        for (int i = 0; i < options.size(); i++) {
            shares[i] = options.get(i).getSharesBought();
        }
        return shares;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommissionPercent() { return commissionPercent; }
    public CommissionType getCommissionType() { return commissionType; }
    public List<EventOption> getOptions() { return options; }
    public TradingMethod getTradingMethod() { return tradingMethod; }
    public User getMarketMaker() { return marketMaker; }
    public Account getAccount() { return account; }
    /** Commission this event has generated for its market maker, across all its trades. */
    public double getCommissionCollected() { return commissionCollected; }
    public List<Trade> getTrades() { return Collections.unmodifiableList(trades); }
    public EventStatus getStatus() { return status; }
    public boolean isActive() { return status == EventStatus.ACTIVE; }
    public EventOption getWinningOption() { return winningOption; }

    private void allocateInitialShares(User mm, OrderBookTradingMethod orderBook, double totalCost) {
        long pairs = orderBook.initialSharePairs();
        if (pairs == 0) {
            return;
        }

        Participation participation = participationOf(mm);
        double costPerOption = totalCost / options.size();
        for (EventOption option : options) {
            participation.recordPurchase(option.getName(), pairs, costPerOption, 0);
            option.addShares(pairs);
        }
    }

    private double payoutPerShare() {
        return tradingMethod instanceof OrderBookTradingMethod orderBook
                ? orderBook.getBaseValue()
                : LMSR_PAYOUT_PER_SHARE;
    }

    private double returnLeftoverToMarketMaker() {
        if (tradingMethod instanceof OrderBookTradingMethod) {
            return 0;
        }
        double leftover = account.getBalance();
        if (leftover <= 0) {
            return 0;
        }
        account.withdraw(leftover);
        marketMaker.credit(leftover);
        return leftover;
    }
}
