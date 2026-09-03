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
import gm.engine.exception.InsufficientFundsException;
import gm.engine.exception.InvalidQuantityException;
import gm.engine.orderbook.Execution;
import gm.engine.orderbook.Order;
import gm.engine.orderbook.OrderBook;
import gm.engine.orderbook.OrderResult;
import gm.engine.orderbook.OrderSide;
import gm.engine.trading.LmsrTradingMethod;
import gm.engine.trading.OrderBookTradingMethod;
import gm.engine.trading.TradingMethod;

public class Event {

    private static final double PERCENT = 100.0;
    private static final double LMSR_PAYOUT_PER_SHARE = 1.0;
    private static final double PRICE_EPSILON = 1e-9;

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
    private final Map<String, OrderBook> books = new LinkedHashMap<>();
    private long orderSequence = 0;

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

        // An order book event gets an empty book per option up front, so the UI can show the books
        // before anyone has traded. LMSR events get none, and submitOrder refuses them anyway.
        if (tradingMethod instanceof OrderBookTradingMethod) {
            for (EventOption option : this.options) {
                books.put(option.getName(), new OrderBook(option.getName()));
            }
        }
        // The account stays empty until a real person opens the event and pays for it.
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

        // Resolution cancels whatever is still resting: there is nothing left to trade.
        for (OrderBook book : books.values()) {
            book.cancelAllOrders();
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

    // ------------------------------------------------------------------ order book trading

    /**
     * Submits one order to this event's order book and processes it immediately.
     * <p>
     * Three steps, in this order: match against the opposite side of this option's own book; then,
     * if minting is allowed and quantity remains, create new pairs against demand for the other
     * option; then rest whatever is still unfilled. An order that matches nothing has not failed -
     * resting in the book is the normal fate of most orders.
     */
    public OrderResult submitOrder(User user, int optionNumber, OrderSide side,
                                   long quantity, double pricePerShare) {
        if (status != EventStatus.ACTIVE) {
            throw new EventNotActiveException(String.format(
                    "Error: event \"%s\" is %s, so orders cannot be submitted.",
                    name, status == EventStatus.NOT_STARTED ? "not open yet" : "closed"));
        }
        if (!(tradingMethod instanceof OrderBookTradingMethod orderBook)) {
            throw new InvalidEventStateException(String.format(
                    "Error: event \"%s\" is an LMSR event - buy shares directly instead of "
                            + "submitting an order.", name));
        }
        user.requireNotBlocked();

        EventOption option = getOption(optionNumber);
        validateOrderPrice(orderBook, pricePerShare);
        if (quantity <= 0) {
            throw new InvalidQuantityException(String.format(
                    "Error: order quantity must be greater than zero, but got %d.", quantity));
        }

        if (side == OrderSide.SELL) {
            requireEnoughShares(user, option, quantity);
        } else {
            requireEnoughMoney(user, quantity, pricePerShare);
        }

        // Participation begins with the first order submitted, whether or not it ever executes.
        participationOf(user);

        Order order = new Order(++orderSequence, user, option.getName(), side, pricePerShare, quantity);
        List<Execution> executions = new ArrayList<>();
        Tally tally = new Tally();

        if (side == OrderSide.BUY) {
            matchBuyAgainstAsks(order, option, executions, tally);
            if (orderBook.allowsMint() && !order.isFilled()) {
                mintAgainstOppositeDemand(order, option, orderBook, executions, tally);
            }
        } else {
            matchSellAgainstBids(order, option, executions, tally);
        }

        long resting = order.getRemainingQuantity();
        if (resting > 0) {
            bookFor(option).rest(order);
        }

        return new OrderResult(quantity, quantity - resting, resting,
                tally.spent, tally.received, tally.commission, executions);
    }

    public OrderBook getOrderBook(String optionName) {
        return books.get(optionName);
    }

    public Map<String, OrderBook> getOrderBooks() {
        return Collections.unmodifiableMap(books);
    }

    // ------------------------------------------------------------------ matching internals

    /** Running totals for the user who submitted the order, so the result can report them. */
    private static final class Tally {
        private double spent;
        private double received;
        private double commission;
    }

    /**
     * A buy sweeps the asks from cheapest up, taking every one priced at or below its limit. Each
     * fill happens at the <em>resting</em> order's price, so the incoming buyer often pays less than
     * they were willing to.
     */
    private void matchBuyAgainstAsks(Order incoming, EventOption option,
                                     List<Execution> executions, Tally tally) {
        OrderBook book = bookFor(option);
        while (!incoming.isFilled()) {
            Order ask = book.bestAsk();
            if (ask == null || ask.getPricePerShare() > incoming.getPricePerShare()) {
                return;
            }

            long quantity = Math.min(incoming.getRemainingQuantity(), ask.getRemainingQuantity());
            double price = ask.getPricePerShare();

            settleTrade(incoming.getUser(), ask.getUser(), option, quantity, price, true, tally);

            incoming.fill(quantity);
            ask.fill(quantity);
            book.recordTrade(price);
            book.removeFilledOrders();
            executions.add(Execution.trade(quantity, incoming.getUserName(), ask.getUserName(),
                    option.getName(), price));
        }
    }

    /** The mirror image: a sell walks the bids from the highest down. */
    private void matchSellAgainstBids(Order incoming, EventOption option,
                                      List<Execution> executions, Tally tally) {
        OrderBook book = bookFor(option);
        while (!incoming.isFilled()) {
            Order bid = book.bestBid();
            if (bid == null || bid.getPricePerShare() < incoming.getPricePerShare()) {
                return;
            }

            long quantity = Math.min(incoming.getRemainingQuantity(), bid.getRemainingQuantity());
            double price = bid.getPricePerShare();

            settleTrade(bid.getUser(), incoming.getUser(), option, quantity, price, false, tally);

            incoming.fill(quantity);
            bid.fill(quantity);
            book.recordTrade(price);
            book.removeFilledOrders();
            executions.add(Execution.trade(quantity, bid.getUserName(), incoming.getUserName(),
                    option.getName(), price));
        }
    }

    /**
     * Creates new share pairs when demand for both options together is worth at least the base
     * value. The resting order is honoured at exactly its own price - it queued first - and the
     * incoming order pays whatever completes the base value, which is never more than its limit.
     * <p>
     * Both payments go to the event account rather than to each other, because these shares did not
     * exist a moment ago: nobody is selling, the event itself is issuing.
     */
    private void mintAgainstOppositeDemand(Order incoming, EventOption option,
                                           OrderBookTradingMethod orderBook,
                                           List<Execution> executions, Tally tally) {
        EventOption other = otherOption(option);
        OrderBook otherBook = bookFor(other);
        double baseValue = orderBook.getBaseValue();

        while (!incoming.isFilled()) {
            Order restingBid = otherBook.bestBid();
            if (restingBid == null
                    || incoming.getPricePerShare() + restingBid.getPricePerShare() < baseValue) {
                return;
            }

            long quantity =
                    Math.min(incoming.getRemainingQuantity(), restingBid.getRemainingQuantity());
            double restingPrice = restingBid.getPricePerShare();
            double incomingPrice = baseValue - restingPrice;

            settleMint(incoming.getUser(), option, incomingPrice,
                    restingBid.getUser(), other, restingPrice, quantity, tally);

            incoming.fill(quantity);
            restingBid.fill(quantity);
            bookFor(option).recordTrade(incomingPrice);
            otherBook.recordTrade(restingPrice);
            otherBook.removeFilledOrders();
            executions.add(Execution.mint(quantity,
                    incoming.getUserName(), option.getName(), incomingPrice,
                    restingBid.getUserName(), other.getName(), restingPrice));
        }
    }

    /**
     * Shares move from seller to buyer and the money moves the other way. The seller receives the
     * full trade value; on-purchase commission is charged to the buyer on top and goes to the
     * market maker. The event account is not involved - no shares were created here.
     *
     * @param buyerIsIncoming whether the buyer is the one who just submitted. The incoming side had
     *                        its funds checked a moment ago, but a resting order was priced when its
     *                        owner could afford it and may no longer be able to - that is the one
     *                        case the exercise wants allowed, reported, and then blocked.
     */
    private void settleTrade(User buyer, User seller, EventOption option, long quantity,
                             double price, boolean buyerIsIncoming, Tally tally) {
        double value = quantity * price;
        double commission = commissionType == CommissionType.ON_PURCHASE
                ? value * commissionPercent / PERCENT
                : 0;

        if (buyerIsIncoming) {
            buyer.charge(value + commission);
        } else {
            buyer.chargeAllowingOverdraft(value + commission);
        }
        seller.credit(value);
        marketMaker.collectCommission(commission);
        commissionCollected += commission;

        participationOf(buyer).recordPurchase(option.getName(), quantity, value, commission);
        participationOf(seller).recordSale(option.getName(), quantity, value, 0);

        if (buyerIsIncoming) {
            tally.spent += value + commission;
            tally.commission += commission;
        } else {
            tally.received += value;
        }
    }

    /** Both buyers pay the event account, and a brand-new share of each option comes into being. */
    private void settleMint(User incomingBuyer, EventOption incomingOption, double incomingPrice,
                            User restingBuyer, EventOption restingOption, double restingPrice,
                            long quantity, Tally tally) {

        double incomingValue = quantity * incomingPrice;
        double restingValue = quantity * restingPrice;
        boolean onPurchase = commissionType == CommissionType.ON_PURCHASE;
        double incomingCommission = onPurchase ? incomingValue * commissionPercent / PERCENT : 0;
        double restingCommission = onPurchase ? restingValue * commissionPercent / PERCENT : 0;

        incomingBuyer.charge(incomingValue + incomingCommission);
        restingBuyer.chargeAllowingOverdraft(restingValue + restingCommission);

        account.deposit(incomingValue + restingValue);
        marketMaker.collectCommission(incomingCommission + restingCommission);
        commissionCollected += incomingCommission + restingCommission;

        participationOf(incomingBuyer)
                .recordPurchase(incomingOption.getName(), quantity, incomingValue, incomingCommission);
        participationOf(restingBuyer)
                .recordPurchase(restingOption.getName(), quantity, restingValue, restingCommission);

        // These shares are new, so the outstanding totals grow. A resale would not touch them.
        incomingOption.addShares(quantity);
        restingOption.addShares(quantity);

        tally.spent += incomingValue + incomingCommission;
        tally.commission += incomingCommission;
    }

    /**
     * Prices are bounded at both ends. A share can never be worth more than the base value it pays
     * out, and an order at zero is not an offer at all.
     */
    private void validateOrderPrice(OrderBookTradingMethod orderBook, double pricePerShare) {
        double minimum = OrderBookTradingMethod.MIN_ORDER_PRICE;
        double maximum = orderBook.maxOrderPrice();
        if (pricePerShare < minimum - PRICE_EPSILON || pricePerShare > maximum + PRICE_EPSILON) {
            throw new InvalidEventDataException(String.format(
                    "Error: price must be between %.2f and %.2f, but got %.2f. A share can never be "
                            + "worth more than the %d it pays out.",
                    minimum, maximum, pricePerShare, orderBook.getBaseValue()));
        }
    }

    private void requireEnoughShares(User user, EventOption option, long quantity) {
        Participation participation = participants.get(user.getName());
        long held = participation == null ? 0 : participation.getShares(option.getName());
        if (held < quantity) {
            throw new InvalidQuantityException(String.format(
                    "Error: %s wants to sell %d shares of \"%s\" but holds only %d.",
                    user.getName(), quantity, option.getName(), held));
        }
    }

    /**
     * Checks the worst case - every share filling at the limit price - so a partly filled order can
     * never run out of money halfway through and leave the event half settled. A mint fills below
     * the limit, so this never refuses an order it should have accepted.
     */
    private void requireEnoughMoney(User user, long quantity, double pricePerShare) {
        double worstCase = quantity * pricePerShare;
        if (commissionType == CommissionType.ON_PURCHASE) {
            worstCase += worstCase * commissionPercent / PERCENT;
        }
        if (!user.canAfford(worstCase)) {
            throw new InsufficientFundsException(String.format(
                    "Error: %s needs up to $%.2f for this order but has only $%.2f available.",
                    user.getName(), worstCase, user.getBalance()));
        }
    }

    private OrderBook bookFor(EventOption option) {
        return books.get(option.getName());
    }

    private EventOption otherOption(EventOption option) {
        return options.get(0).equals(option) ? options.get(1) : options.get(0);
    }
}
