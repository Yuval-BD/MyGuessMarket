package gm.engine.dto;

/**
 * One thing that happened while an order was processed: either shares changing hands between two
 * users, or a new pair being created for two buyers of opposite options.
 * <p>
 * Written from the point of view of whoever submitted the order. {@code price} is what they paid or
 * received per share; the counterparty is the person on the other side. On a mint the two sides pay
 * different amounts for opposite options, which is why the counterparty's option and price are
 * carried separately.
 */
public final class ExecutionDto {

    private final boolean mint;
    private final long quantity;
    private final double price;
    private final String counterpartyName;
    private final String counterpartyOptionName;
    private final double counterpartyPrice;

    public ExecutionDto(boolean mint, long quantity, double price,
                        String counterpartyName, String counterpartyOptionName,
                        double counterpartyPrice) {
        this.mint = mint;
        this.quantity = quantity;
        this.price = price;
        this.counterpartyName = counterpartyName;
        this.counterpartyOptionName = counterpartyOptionName;
        this.counterpartyPrice = counterpartyPrice;
    }

    /** True when new shares were created; false when existing shares changed hands. */
    public boolean isMint() { return mint; }

    public long getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getCounterpartyName() { return counterpartyName; }
    public String getCounterpartyOptionName() { return counterpartyOptionName; }
    public double getCounterpartyPrice() { return counterpartyPrice; }
}
