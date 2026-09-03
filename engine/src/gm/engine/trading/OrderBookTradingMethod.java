package gm.engine.trading;

import gm.engine.exception.InvalidEventDataException;

public final class OrderBookTradingMethod implements TradingMethod{

    /** No order may be priced below one cent - an order at zero is not an offer. */
    public static final double MIN_ORDER_PRICE = 0.01;

    private final int initialInvestment;
    private final int baseValue;
    private final boolean allowMint;

    public  OrderBookTradingMethod(int initialInvestment, int baseValue, boolean allowMint) {
        if (baseValue <= 0) {
            throw new InvalidEventDataException(String.format(
                    "Error: base value d must be a positive integer, but found %d.", baseValue));
        }

        if (initialInvestment < 0) {
            throw new InvalidEventDataException(String.format(
                    "Error: initial investment value cannot be a negative integer, but found %d.", initialInvestment));
        }

        this.initialInvestment = initialInvestment;
        this.baseValue = baseValue;
        this.allowMint = allowMint;
    }

    @Override
    public TradingMethodType kind() {
        return TradingMethodType.ORDER_BOOK;
    }

    @Override
    public double openingInvestment(int numberOfOptions) {
        return initialInvestment;
    }

    public long initialSharePairs() {
        return initialInvestment / baseValue;
    }

    public int getInitialInvestment()  { return initialInvestment; }
    public int getBaseValue()          { return baseValue; }
    public boolean allowsMint()        { return allowMint; }
    public double maxOrderPrice()      { return baseValue - 0.01; }
}
