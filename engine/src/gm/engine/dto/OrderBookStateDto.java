package gm.engine.dto;

import java.util.List;

/**
 * Everything the order-book view of one event needs: both books, the event's own money, and who is
 * taking part. The counterpart to EventStateDto, which serves LMSR events - the two mechanisms show
 * genuinely different things, so they get their own shapes rather than one union of both.
 */
public final class OrderBookStateDto {

    private final int eventId;
    private final String eventName;
    private final EventStatusDto status;
    private final String marketMakerName;
    private final double accountBalance;
    private final double commissionCollected;
    private final int baseValue;
    private final boolean allowMint;
    private final List<OptionBookDto> books;
    private final List<ParticipantDto> participants;
    private final String winningOptionName;

    public OrderBookStateDto(int eventId, String eventName, EventStatusDto status,
                             String marketMakerName, double accountBalance,
                             double commissionCollected, int baseValue, boolean allowMint,
                             List<OptionBookDto> books, List<ParticipantDto> participants,
                             String winningOptionName) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.marketMakerName = marketMakerName;
        this.accountBalance = accountBalance;
        this.commissionCollected = commissionCollected;
        this.baseValue = baseValue;
        this.allowMint = allowMint;
        this.books = List.copyOf(books);
        this.participants = List.copyOf(participants);
        this.winningOptionName = winningOptionName;
    }

    public int getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public EventStatusDto getStatus() { return status; }
    public String getMarketMakerName() { return marketMakerName; }
    public double getAccountBalance() { return accountBalance; }
    public double getCommissionCollected() { return commissionCollected; }

    /** What one winning share pays out, and the price two crossing bids must reach to mint. */
    public int getBaseValue() { return baseValue; }

    public boolean isAllowMint() { return allowMint; }
    public List<OptionBookDto> getBooks() { return books; }
    public List<ParticipantDto> getParticipants() { return participants; }

    /** Null while the event is still open. */
    public String getWinningOptionName() { return winningOptionName; }
}
