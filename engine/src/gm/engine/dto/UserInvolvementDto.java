package gm.engine.dto;

import java.util.List;

/**
 * One user's stake in one event: what they own, what it cost, and whether they run the event.
 * <p>
 * This is what the users screen lists - "the events this user is in" - and the exercise counts a
 * user as involved from their first action in the event, so an involvement with no holdings at all
 * is normal rather than a sign of a bug.
 */
public final class UserInvolvementDto {

    private final int eventId;
    private final String eventName;
    private final TradingMethodTypeDto methodType;
    private final EventStatusDto status;
    private final boolean marketMaker;
    private final boolean participating;
    private final List<HoldingDto> holdings;
    private final double totalCommissionPaid;
    private final double netResult;

    public UserInvolvementDto(int eventId, String eventName, TradingMethodTypeDto methodType,
                              EventStatusDto status, boolean marketMaker, boolean participating,
                              List<HoldingDto> holdings,
                              double totalCommissionPaid, double netResult) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.methodType = methodType;
        this.status = status;
        this.marketMaker = marketMaker;
        this.participating = participating;
        this.holdings = List.copyOf(holdings);
        this.totalCommissionPaid = totalCommissionPaid;
        this.netResult = netResult;
    }

    public int getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public TradingMethodTypeDto getMethodType() { return methodType; }
    public EventStatusDto getStatus() { return status; }

    /** True when this user is the market maker running the event. */
    public boolean isMarketMaker() { return marketMaker; }

    /** True once this user has actually acted in the event, as opposed to merely being able to. */
    public boolean isParticipating() { return participating; }

    public List<HoldingDto> getHoldings() { return holdings; }
    public double getTotalCommissionPaid() { return totalCommissionPaid; }

    /** Money received minus money spent in this event. Only meaningful once it has closed. */
    public double getNetResult() { return netResult; }
}
