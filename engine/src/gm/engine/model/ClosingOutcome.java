package gm.engine.model;

/**
 * What actually happened when an event was closed. Returned by {@link Event#close} so the engine
 * can report it without having to re-derive figures that only existed during the settlement -
 * the leftover in particular is computed and paid out in one step and is not stored anywhere.
 *
 * @param winningOptionName        the option that was declared the winner
 * @param totalPaidToWinners       gross paid out of the event account, before commission
 * @param totalCommissionCollected on-close commission moved to the market maker (0 for on-purchase events)
 * @param leftoverReturnedToMarketMaker  remaining subsidy returned to the MM (LMSR only, otherwise 0)
 */
public record ClosingOutcome(String winningOptionName,
                             double totalPaidToWinners,
                             double totalCommissionCollected,
                             double leftoverReturnedToMarketMaker) {
}
