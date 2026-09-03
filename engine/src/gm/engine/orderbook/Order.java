package gm.engine.orderbook;

import gm.engine.exception.InvalidQuantityException;
import gm.engine.model.User;

/**
 * One instruction to buy or sell shares of one option at a stated price per share.
 * <p>
 * An order keeps its original quantity as well as what is left of it, so a partly filled order can
 * still show what it set out to do. The sequence number is the arrival order, which breaks ties
 * between orders at the same price: whoever queued first is served first.
 */
public class Order {

    private final long sequence;
    private final User user;
    private final String optionName;
    private final OrderSide side;
    private final double pricePerShare;
    private final long originalQuantity;

    private long remainingQuantity;

    public Order(long sequence, User user, String optionName, OrderSide side,
                 double pricePerShare, long quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(String.format(
                    "Error: order quantity must be greater than zero, but got %d.", quantity));
        }
        this.sequence = sequence;
        this.user = user;
        this.optionName = optionName;
        this.side = side;
        this.pricePerShare = pricePerShare;
        this.originalQuantity = quantity;
        this.remainingQuantity = quantity;
    }

    public void fill(long quantity) {
        if (quantity <= 0 || quantity > remainingQuantity) {
            throw new InvalidQuantityException(String.format(
                    "Error: cannot fill %d shares of an order with %d remaining.",
                    quantity, remainingQuantity));
        }
        remainingQuantity -= quantity;
    }

    public boolean isFilled() {
        return remainingQuantity == 0;
    }

    public long getSequence() { return sequence; }
    public User getUser() { return user; }
    public String getUserName() { return user.getName(); }
    public String getOptionName() { return optionName; }
    public OrderSide getSide() { return side; }
    public double getPricePerShare() { return pricePerShare; }
    public long getOriginalQuantity() { return originalQuantity; }
    public long getRemainingQuantity() { return remainingQuantity; }
}
