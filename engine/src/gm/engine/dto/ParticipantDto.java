package gm.engine.dto;

import java.util.List;

/** One person's position in an event, as shown in the event's participants panel. */
public final class ParticipantDto {

    private final String userName;
    private final boolean marketMaker;
    private final List<HoldingDto> holdings;
    private final double totalCommissionPaid;

    public ParticipantDto(String userName, boolean marketMaker, List<HoldingDto> holdings,
                          double totalCommissionPaid) {
        this.userName = userName;
        this.marketMaker = marketMaker;
        this.holdings = List.copyOf(holdings);
        this.totalCommissionPaid = totalCommissionPaid;
    }

    public String getUserName() { return userName; }
    public boolean isMarketMaker() { return marketMaker; }
    public List<HoldingDto> getHoldings() { return holdings; }
    public double getTotalCommissionPaid() { return totalCommissionPaid; }
}
