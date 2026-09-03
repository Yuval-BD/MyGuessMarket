package gm.engine.orderbook;

public enum ExecutionKind {
    /** Shares changed hands between two users. Money goes to the seller. */
    TRADE,
    /** A new pair of shares was created for two buyers. Money goes to the event account. */
    MINT
}
