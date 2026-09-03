package gm.engine.orderbook;

/**
 * The price indicators for one option's book. Every field is nullable, and null genuinely means
 * "there is no such number yet" rather than zero - a book with no sellers has no ask, and showing
 * $0.00 for it would read as "free" instead of "nobody is offering".
 *
 * @param lastTradePrice price of the most recent execution, or null if nothing has traded
 * @param bestBid        highest resting buy price, or null if nobody is bidding
 * @param bestAsk        lowest resting sell price, or null if nobody is offering
 * @param mid            midpoint between bid and ask, or null unless both exist
 * @param spread         ask minus bid, or null unless both exist
 */
public record BookStats(Double lastTradePrice, Double bestBid, Double bestAsk,
                        Double mid, Double spread) {
}
