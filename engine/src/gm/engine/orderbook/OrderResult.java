package gm.engine.orderbook;

import java.util.List;

/**
 * What became of one submitted order.
 * <p>
 * The three quantities always add up: {@code filled + resting = requested}. An order that matched
 * nothing is not a failure - it simply rests in the book waiting for someone to take the other
 * side, which is the normal state of most orders in a quiet market.
 */
public record OrderResult(long requestedQuantity,
                          long filledQuantity,
                          long restingQuantity,
                          double totalSpent,
                          double totalReceived,
                          double commissionPaid,
                          List<Execution> executions) {

    public OrderResult {
        executions = List.copyOf(executions);
    }
}
