package gm.engine.dto;

import java.util.List;

/**
 * Everything the order-book view of one event needs: both books, who is taking part, and the two
 * rules that shape trading - what a winning share pays and whether minting is allowed.
 * <p>
 * The counterpart to LmsrStateDto, which serves LMSR events - the two mechanisms show genuinely
 * different things, so they get their own shapes rather than one union of both. Neither repeats the
 * event's name, status or market maker: the caller already holds an EventDto for that, and a second
 * copy is a second thing to keep in step.
 */
public final class OrderBookStateDto {

    private final double commissionCollected;
    private final int baseValue;
    private final boolean allowMint;
    private final List<OptionBookDto> books;
    private final List<ParticipantDto> participants;

    public OrderBookStateDto(double commissionCollected, int baseValue, boolean allowMint,
                             List<OptionBookDto> books, List<ParticipantDto> participants) {
        this.commissionCollected = commissionCollected;
        this.baseValue = baseValue;
        this.allowMint = allowMint;
        this.books = List.copyOf(books);
        this.participants = List.copyOf(participants);
    }

    /** Commission this event has generated for its market maker, across every trade and mint. */
    public double getCommissionCollected() { return commissionCollected; }

    /** What one winning share pays out, and the price two crossing bids must reach to mint. */
    public int getBaseValue() { return baseValue; }

    public boolean isAllowMint() { return allowMint; }
    public List<OptionBookDto> getBooks() { return books; }
    public List<ParticipantDto> getParticipants() { return participants; }
}
