package gm.engine.orderbook;

/**
 * One thing that actually happened while an order was being processed, kept so the UI can report it.
 * <p>
 * A TRADE has a buyer and a seller of the same option at one price. A MINT has two buyers, of
 * opposite options, at prices that add up to the base value - so both are described here as a
 * "party" and a "counterparty" with their own option and their own price.
 */
public record Execution(ExecutionKind kind,
                        long quantity,
                        String partyName, String partyOptionName, double partyPrice,
                        String counterpartyName, String counterpartyOptionName,
                        double counterpartyPrice) {

    public static Execution trade(long quantity, String buyerName, String sellerName,
                                  String optionName, double price) {
        return new Execution(ExecutionKind.TRADE, quantity,
                buyerName, optionName, price,
                sellerName, optionName, price);
    }

    public static Execution mint(long quantity,
                                 String incomingBuyer, String incomingOption, double incomingPrice,
                                 String restingBuyer, String restingOption, double restingPrice) {
        return new Execution(ExecutionKind.MINT, quantity,
                incomingBuyer, incomingOption, incomingPrice,
                restingBuyer, restingOption, restingPrice);
    }
}
