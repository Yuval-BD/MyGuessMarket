package gm.engine.dto;

import java.util.List;

/** One option's whole book: buyers, sellers, its price indicators, and how many shares exist. */
public final class OptionBookDto {

    private final String optionName;
    private final List<RestingOrderDto> bids;
    private final List<RestingOrderDto> asks;
    private final BookStatsDto stats;
    private final long sharesOutstanding;

    public OptionBookDto(String optionName, List<RestingOrderDto> bids, List<RestingOrderDto> asks,
                         BookStatsDto stats, long sharesOutstanding) {
        this.optionName = optionName;
        this.bids = List.copyOf(bids);
        this.asks = List.copyOf(asks);
        this.stats = stats;
        this.sharesOutstanding = sharesOutstanding;
    }

    public String getOptionName() { return optionName; }

    /** Highest price first - the order a seller would hit next is at the top. */
    public List<RestingOrderDto> getBids() { return bids; }

    /** Lowest price first - the order a buyer would lift next is at the top. */
    public List<RestingOrderDto> getAsks() { return asks; }

    public BookStatsDto getStats() { return stats; }
    public long getSharesOutstanding() { return sharesOutstanding; }
}
