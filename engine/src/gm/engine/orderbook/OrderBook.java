package gm.engine.orderbook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The resting orders for one option: buyers on one side, sellers on the other.
 * <p>
 * Both sides are kept in the order they will be consumed, so "the next order to match against" is
 * always the first element. Bids run from the highest price down, asks from the lowest price up,
 * and orders at the same price are served in arrival order - the first to queue is the first to
 * trade, which is what makes the resting order's price the one that gets honoured.
 * <p>
 * This class only holds and orders the book. It moves no money and owns no rules about when a
 * match is allowed; the event decides that and settles it.
 */
public class OrderBook {

    /** Highest price first; among equal prices, whoever arrived first. */
    private static final Comparator<Order> BID_ORDER =
            Comparator.comparingDouble(Order::getPricePerShare).reversed()
                    .thenComparingLong(Order::getSequence);

    /** Lowest price first; among equal prices, whoever arrived first. */
    private static final Comparator<Order> ASK_ORDER =
            Comparator.comparingDouble(Order::getPricePerShare)
                    .thenComparingLong(Order::getSequence);

    private final String optionName;
    private final List<Order> bids = new ArrayList<>();
    private final List<Order> asks = new ArrayList<>();

    private Double lastTradePrice;

    public OrderBook(String optionName) {
        this.optionName = optionName;
    }

    public String getOptionName() {
        return optionName;
    }

    /** Adds an order that could not be matched, in its correct queue position. */
    public void rest(Order order) {
        List<Order> side = order.getSide() == OrderSide.BUY ? bids : asks;
        side.add(order);
        side.sort(order.getSide() == OrderSide.BUY ? BID_ORDER : ASK_ORDER);
    }

    /** The order a seller would hit first, or null when nobody is bidding. */
    public Order bestBid() {
        return bids.isEmpty() ? null : bids.getFirst();
    }

    /** The order a buyer would lift first, or null when nobody is offering. */
    public Order bestAsk() {
        return asks.isEmpty() ? null : asks.getFirst();
    }

    public void recordTrade(double price) {
        lastTradePrice = price;
    }

    /** Drops fully filled orders from both sides. Call after a round of matching. */
    public void removeFilledOrders() {
        bids.removeIf(Order::isFilled);
        asks.removeIf(Order::isFilled);
    }

    /**
     * Clears both sides. Closing an event cancels every order still resting in it - the book does
     * not survive resolution.
     */
    public void cancelAllOrders() {
        bids.clear();
        asks.clear();
    }

    public List<Order> getBids() {
        return Collections.unmodifiableList(bids);
    }

    public List<Order> getAsks() {
        return Collections.unmodifiableList(asks);
    }

    /**
     * Mid and spread exist only when both sides are populated. With one side empty there is no
     * meaningful midpoint, so these stay null rather than being invented from the one price
     * that does exist.
     */
    public BookStats stats() {
        Order bid = bestBid();
        Order ask = bestAsk();
        Double bestBidPrice = bid == null ? null : bid.getPricePerShare();
        Double bestAskPrice = ask == null ? null : ask.getPricePerShare();

        Double mid = null;
        Double spread = null;
        if (bestBidPrice != null && bestAskPrice != null) {
            mid = (bestBidPrice + bestAskPrice) / 2;
            spread = bestAskPrice - bestBidPrice;
        }

        return new BookStats(lastTradePrice, bestBidPrice, bestAskPrice, mid, spread);
    }
}
