package gm.engine.dto;

/**
 * The five price indicators for one option.
 * <p>
 * Every one is a nullable Double, and null means "there is no such number", not zero. A book with
 * no sellers has no ask at all; showing $0.00 would read as "free" rather than "nobody is offering".
 * Mid and spread need both sides, so they are null unless a bid and an ask both exist.
 */
public final class BookStatsDto {

    private final Double lastTradePrice;
    private final Double bestBid;
    private final Double bestAsk;
    private final Double mid;
    private final Double spread;

    public BookStatsDto(Double lastTradePrice, Double bestBid, Double bestAsk,
                        Double mid, Double spread) {
        this.lastTradePrice = lastTradePrice;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.mid = mid;
        this.spread = spread;
    }

    public Double getLastTradePrice() { return lastTradePrice; }
    public Double getBestBid() { return bestBid; }
    public Double getBestAsk() { return bestAsk; }
    public Double getMid() { return mid; }
    public Double getSpread() { return spread; }
}
