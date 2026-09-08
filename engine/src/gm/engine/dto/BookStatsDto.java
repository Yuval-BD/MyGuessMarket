package gm.engine.dto;

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
