package gm.engine.dto;

/**
 * One thing that happened while an order was processed: either shares changing hands between two
 * users, or a new pair being created for two buyers of opposite options.
 */
public final class ExecutionDto {

    private final boolean mint;
    private final long quantity;
    private final String partyName;
    private final String partyOptionName;
    private final double partyPrice;
    private final String counterpartyName;
    private final String counterpartyOptionName;
    private final double counterpartyPrice;

    public ExecutionDto(boolean mint, long quantity,
                        String partyName, String partyOptionName, double partyPrice,
                        String counterpartyName, String counterpartyOptionName,
                        double counterpartyPrice) {
        this.mint = mint;
        this.quantity = quantity;
        this.partyName = partyName;
        this.partyOptionName = partyOptionName;
        this.partyPrice = partyPrice;
        this.counterpartyName = counterpartyName;
        this.counterpartyOptionName = counterpartyOptionName;
        this.counterpartyPrice = counterpartyPrice;
    }

    /** True when new shares were created; false when existing shares changed hands. */
    public boolean isMint() { return mint; }

    public long getQuantity() { return quantity; }
    public String getPartyName() { return partyName; }
    public String getPartyOptionName() { return partyOptionName; }
    public double getPartyPrice() { return partyPrice; }
    public String getCounterpartyName() { return counterpartyName; }
    public String getCounterpartyOptionName() { return counterpartyOptionName; }
    public double getCounterpartyPrice() { return counterpartyPrice; }
}
