package gm.engine.dto;

/** One order waiting in a book: who placed it, how much is left of it, and at what price. */
public final class RestingOrderDto {

    private final String userName;
    private final long quantity;
    private final double pricePerShare;

    public RestingOrderDto(String userName, long quantity, double pricePerShare) {
        this.userName = userName;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
    }

    public String getUserName() { return userName; }

    /** What remains unfilled - a partly filled order shows only the part still on offer. */
    public long getQuantity() { return quantity; }

    public double getPricePerShare() { return pricePerShare; }
}
