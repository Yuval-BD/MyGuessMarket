package gm.engine.orderbook;

/**
 * One thing that actually happened while an order was being processed, kept so the UI can report it.
 * <p>
 * Always written from the point of view of whoever submitted the order: {@code price} is what they
 * paid or received per share, and the counterparty is the person on the other side. A TRADE has one
 * price for both sides. A MINT has two buyers of opposite options at prices that add up to the base
 * value, so there the counterparty's option and price differ and are reported separately.
 */
public record Execution(ExecutionKind kind,
                        long quantity,
                        double price,
                        String counterpartyName, String counterpartyOptionName,
                        double counterpartyPrice) {

    /** Shares changed hands at one price, so both sides see the same number. */
    public static Execution trade(long quantity, double price,
                                  String counterpartyName, String optionName) {
        return new Execution(ExecutionKind.TRADE, quantity, price,
                counterpartyName, optionName, price);
    }

    /** New shares were created, so the two buyers paid different amounts for opposite options. */
    public static Execution mint(long quantity, double price,
                                 String counterpartyName, String counterpartyOptionName,
                                 double counterpartyPrice) {
        return new Execution(ExecutionKind.MINT, quantity, price,
                counterpartyName, counterpartyOptionName, counterpartyPrice);
    }
}
