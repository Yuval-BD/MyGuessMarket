package gm.engine.trading;

public sealed interface TradingMethod permits LmsrTradingMethod, OrderBookTradingMethod {
    TradingMethodType kind();
    double openingInvestment(int numberOfOptions);
}