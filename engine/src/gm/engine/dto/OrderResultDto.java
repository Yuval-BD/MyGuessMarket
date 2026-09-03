package gm.engine.dto;

import java.util.List;

/**
 * What became of one submitted order. {@code filled + resting = requested}, always.
 * <p>
 * An order that filled nothing has not failed - resting in the book until someone takes the other
 * side is the normal state of most orders.
 */
public final class OrderResultDto {

    private final long requestedQuantity;
    private final long filledQuantity;
    private final long restingQuantity;
    private final double totalSpent;
    private final double totalReceived;
    private final double commissionPaid;
    private final List<ExecutionDto> executions;

    public OrderResultDto(long requestedQuantity, long filledQuantity, long restingQuantity,
                          double totalSpent, double totalReceived, double commissionPaid,
                          List<ExecutionDto> executions) {
        this.requestedQuantity = requestedQuantity;
        this.filledQuantity = filledQuantity;
        this.restingQuantity = restingQuantity;
        this.totalSpent = totalSpent;
        this.totalReceived = totalReceived;
        this.commissionPaid = commissionPaid;
        this.executions = List.copyOf(executions);
    }

    public long getRequestedQuantity() { return requestedQuantity; }
    public long getFilledQuantity() { return filledQuantity; }
    public long getRestingQuantity() { return restingQuantity; }
    public double getTotalSpent() { return totalSpent; }
    public double getTotalReceived() { return totalReceived; }
    public double getCommissionPaid() { return commissionPaid; }
    public List<ExecutionDto> getExecutions() { return executions; }
}
