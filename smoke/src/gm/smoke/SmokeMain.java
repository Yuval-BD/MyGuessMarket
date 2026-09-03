package gm.smoke;

import gm.engine.GuessMarketEngine;
import gm.engine.GuessMarketEngineImpl;
import gm.engine.dto.CloseResultDto;
import gm.engine.dto.OrderResultDto;
import gm.engine.dto.OrderSideDto;
import gm.engine.dto.EventDto;
import gm.engine.dto.EventStateDto;
import gm.engine.dto.OptionStateDto;
import gm.engine.dto.PurchaseResultDto;
import gm.engine.dto.UserDto;
import gm.engine.exception.GuessMarketException;
import gm.engine.exception.XmlContentException;

/**
 * Scripted smoke test for the engine. Replaces the exercise-1 interactive console.
 * <p>
 * Runs one fixed scenario against {@code small.xml} and checks every figure against a hand
 * calculation. It exists so that a money-flow regression shows up in one second without
 * clicking through a GUI, and so that the engine keeps a second, independent consumer
 * besides JavaFX - which is the evidence that the engine really is passive.
 * <p>
 * All printing in this program lives in this class. The engine prints nothing, ever.
 */
public final class SmokeMain {

    private static final String XML_FOLDER =
            "C:\\Users\\yuvib\\OneDrive\\Desktop\\JAVA Projects\\Project 2\\AI Info\\";

    private static final String DEFAULT_XML = XML_FOLDER + "small.xml";

    /** Event 1 in small.xml: "Mujtaba is Dead", LMSR with b=100, commission 5% on-purchase. */
    private static final int LMSR_EVENT_ID = 1;
    private static final String MARKET_MAKER = "Tikva";
    private static final String BUYER = "Menash";
    private static final int OPTION_ONE = 1;
    private static final long QUANTITY = 100;

    /** Tikva 10000 + Menash 100 + Avrum 1000. Money only moves, so this total never changes. */
    private static final double LMSR_TOTAL_CASH = 11100.00;

    /** The simulator's four traders: Zoe 500 + Alice 200 + Bob 200 + Carol 200. */
    private static final String CLOB_XML_NAME = "clob-sim.xml";
    private static final double CLOB_TOTAL_CASH = 1100.00;
    private static final int CLOB_EVENT_ID = 1;
    private static final int YES = 1;
    private static final int NO = 2;

    private static final double TOLERANCE = 0.01;

    private static int checksRun;
    private static int checksFailed;

    private SmokeMain() {
    }

    public static void main(String[] args) {
        String xmlPath = args.length > 0 ? args[0] : DEFAULT_XML;
        GuessMarketEngine engine = new GuessMarketEngineImpl();

        try {
            runScenario(engine, xmlPath);
            runRejectionScenario(engine);
            runOrderBookScenario();
        } catch (GuessMarketException e) {
            System.out.println();
            System.out.println("ABORTED - the engine rejected something: " + e.getMessage());
            checksFailed++;
        }

        System.out.println();
        System.out.printf("%d checks, %d failed.%n", checksRun, checksFailed);
        System.out.println(checksFailed == 0
                ? "ALL GOOD"
                : "SOMETHING IS WRONG - see the FAIL lines above");
    }

    private static void runScenario(GuessMarketEngine engine, String xmlPath) {

        heading("Load " + xmlPath);
        engine.loadEventsFromFile(xmlPath);
        System.out.printf("  %d events, %d users loaded.%n",
                engine.getAllEvents().size(), engine.getAllUsers().size());
        check("Tikva starting balance", balanceOf(engine, MARKET_MAKER), 10000.00);
        check("Menash starting balance", balanceOf(engine, BUYER), 100.00);
        check("event account before open", accountOf(engine, LMSR_EVENT_ID), 0.00);
        checkConservation(engine, "after load");

        heading("Tikva opens event " + LMSR_EVENT_ID);
        engine.openEvent(LMSR_EVENT_ID, MARKET_MAKER);
        // subsidy = b * ln(2) = 100 * 0.693147 = 69.31, paid by the MM into the event account
        check("subsidy moved to event account", accountOf(engine, LMSR_EVENT_ID), 69.31);
        check("Tikva after paying subsidy", balanceOf(engine, MARKET_MAKER), 9930.69);
        checkConservation(engine, "after open");

        heading("Prices before any trade");
        printOptionStates(engine);
        check("option 1 opening price", priceOf(engine, 0), 0.50);

        heading("Menash buys " + QUANTITY + " of option " + OPTION_ONE);
        PurchaseResultDto purchase =
                engine.buyLmsrShares(LMSR_EVENT_ID, BUYER, OPTION_ONE, QUANTITY);
        // C(100,0) - C(0,0) = 100*ln(e+1) - 100*ln(2) = 131.33 - 69.31 = 62.01
        check("cost of the shares", purchase.getSharesCost(), 62.01);
        check("commission at 5%", purchase.getCommissionPaid(), 3.10);
        check("total paid", purchase.getTotalPaid(), 65.11);
        check("Menash after buying", balanceOf(engine, BUYER), 34.89);
        // The commission goes to the MM's own account, NOT the event account.
        // This is the single most important difference from exercise 1.
        check("commission landed on the MM", balanceOf(engine, MARKET_MAKER), 9933.79);
        check("event account holds shares money only", accountOf(engine, LMSR_EVENT_ID), 131.33);
        checkConservation(engine, "after buy");

        heading("Prices after the trade");
        printOptionStates(engine);
        check("option 1 price after", priceOf(engine, 0), 0.73);
        check("option 2 price after", priceOf(engine, 1), 0.27);

        heading("Tikva closes event " + LMSR_EVENT_ID + ", option " + OPTION_ONE + " wins");
        CloseResultDto close = engine.closeEvent(LMSR_EVENT_ID, MARKET_MAKER, OPTION_ONE);
        // 100 winning shares x $1. Commission is on-purchase here, so nothing more is taken now.
        check("paid out to winners", close.getTotalPaidToWinners(), 100.00);
        check("leftover returned to the MM", close.getLeftoverReturnedToMarketMaker(), 31.33);
        check("Menash after payout", balanceOf(engine, BUYER), 134.89);
        check("Tikva after leftover returns", balanceOf(engine, MARKET_MAKER), 9965.11);
        check("event account emptied", accountOf(engine, LMSR_EVENT_ID), 0.00);
        checkConservation(engine, "after close");
    }

    /**
     * The other half of loading: a file that is schema-valid but application-invalid must be
     * rejected with readable reasons, and must leave the system already loaded untouched.
     * <p>
     * That second part is the one worth guarding. It is easy to write a loader that mutates as it
     * parses, and the damage only shows up as a half-replaced system some time later.
     */
    private static void runRejectionScenario(GuessMarketEngine engine) {

        // The good file is still loaded from the first scenario, and its event 1 is now closed
        // with Menash holding 100 winning shares. Nothing below may disturb any of that.
        double menashBefore = balanceOf(engine, BUYER);
        double tikvaBefore = balanceOf(engine, MARKET_MAKER);
        int eventsBefore = engine.getAllEvents().size();

        expectRejection(engine, XML_FOLDER + "error-2.xml",
                "user with zero initial cash");
        expectRejection(engine, XML_FOLDER + "error-3.xml",
                "market maker pointing at an event that does not exist");
        expectRejection(engine, XML_FOLDER + "does-not-exist.xml",
                "missing file");
        expectRejection(engine, XML_FOLDER + "small.txt",
                "wrong extension");

        heading("The good file survived every rejection");
        check("events still loaded", engine.getAllEvents().size(), eventsBefore);
        check("Menash balance untouched", balanceOf(engine, BUYER), menashBefore);
        check("Tikva balance untouched", balanceOf(engine, MARKET_MAKER), tikvaBefore);
        check("loaded path still the good file",
                engine.getLoadedFilePath().endsWith("small.xml") ? 1 : 0, 1);
        checkConservation(engine, "after rejections");
    }

    /** Loads a file that must fail, and prints why the engine refused it. */
    private static void expectRejection(GuessMarketEngine engine, String path, String because) {
        heading("Reject: " + because);
        checksRun++;
        try {
            engine.loadEventsFromFile(path);
            checksFailed++;
            System.out.println("  [FAIL] the file loaded, but it should have been rejected.");
        } catch (XmlContentException e) {
            System.out.println("  [OK  ] rejected, listing every problem at once:");
            for (String problem : e.getProblems()) {
                System.out.println("           - " + problem);
            }
        } catch (GuessMarketException e) {
            System.out.println("  [OK  ] rejected: " + e.getMessage());
        }
    }

    /**
     * Replays the professor's CLOB simulation move for move, on a fresh engine.
     * <p>
     * Every figure checked here comes from the simulator's own ledger, so this is the closest thing
     * to an authoritative test of the matching engine: resting-price execution, a sell walking two
     * bids, a partial fill, a peer-to-peer mint, and the commission going to the market maker.
     */
    private static void runOrderBookScenario() {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        String path = XML_FOLDER + CLOB_XML_NAME;

        heading("ORDER BOOK - replaying the professor's simulation");
        engine.loadEventsFromFile(path);
        checkConservation(engine, "at the start", CLOB_TOTAL_CASH);

        // Step 1: Zoe deposits $100 and receives 100 pairs.
        engine.openEvent(CLOB_EVENT_ID, "Zoe");
        check("Zoe after minting 100 pairs", balanceOf(engine, "Zoe"), 400.00);
        check("event account holds the deposit", accountOf(engine, CLOB_EVENT_ID), 100.00);

        // Steps 2-6: the book fills up with orders that cannot cross.
        resting(engine, "Bob   bids  20 Yes @0.50", "Bob", YES, OrderSideDto.BUY, 20, 0.50, 20);
        resting(engine, "Carol bids  15 Yes @0.48", "Carol", YES, OrderSideDto.BUY, 15, 0.48, 15);
        resting(engine, "Zoe   asks  25 Yes @0.58", "Zoe", YES, OrderSideDto.SELL, 25, 0.58, 25);
        resting(engine, "Zoe   asks  15 Yes @0.65", "Zoe", YES, OrderSideDto.SELL, 15, 0.65, 15);

        // Steps 7-8: Alice lifts the cheap ask. A plain resale - Zoe keeps the money.
        heading("Alice buys 25 Yes @0.58 - matches Zoe's ask");
        OrderResultDto lift = engine.submitOrder(CLOB_EVENT_ID, "Alice", YES, OrderSideDto.BUY, 25, 0.58);
        check("filled", lift.getFilledQuantity(), 25);
        check("Alice spent 14.50 + 0.145 commission", lift.getTotalSpent(), 14.65);
        check("Alice balance", balanceOf(engine, "Alice"), 185.36);
        check("Zoe collected sale plus commission", balanceOf(engine, "Zoe"), 414.65);
        check("event account untouched by a resale", accountOf(engine, CLOB_EVENT_ID), 100.00);
        checkConservation(engine, "after the resale", CLOB_TOTAL_CASH);

        // Steps 9-11: the same on the No book, leaving Zoe's ask partly filled.
        resting(engine, "Zoe   asks  50 No  @0.45", "Zoe", NO, OrderSideDto.SELL, 50, 0.45, 50);
        heading("Bob buys 25 No @0.45 - a partial fill of Zoe's ask");
        OrderResultDto partial = engine.submitOrder(CLOB_EVENT_ID, "Bob", NO, OrderSideDto.BUY, 25, 0.45);
        check("filled", partial.getFilledQuantity(), 25);
        check("Bob balance", balanceOf(engine, "Bob"), 188.64);

        // Steps 12-13: Zoe's sell walks two bids and beats her own floor price.
        heading("Zoe sells 30 Yes @0.45 - walks Bob's 0.50 then Carol's 0.48");
        OrderResultDto walk = engine.submitOrder(CLOB_EVENT_ID, "Zoe", YES, OrderSideDto.SELL, 30, 0.45);
        check("filled", walk.getFilledQuantity(), 30);
        check("two fills at two prices", walk.getExecutions().size(), 2);
        // 20 x 0.50 + 10 x 0.48 = 14.80, better than the 0.45 she would have accepted.
        check("Zoe received", walk.getTotalReceived(), 14.80);
        check("Bob balance", balanceOf(engine, "Bob"), 178.54);
        check("Carol balance", balanceOf(engine, "Carol"), 195.15);

        // Step 14: Carol's No bid is too low to cross and too low to mint (0.42 + 0.48 < 1.00).
        resting(engine, "Carol bids  35 No  @0.42", "Carol", NO, OrderSideDto.BUY, 35, 0.42, 35);

        // Steps 15-16: Alice's bid plus Carol's resting bid clear the base value, so pairs are minted.
        heading("Alice buys 40 Yes @0.62 - mints 35 pairs against Carol's 0.42 bid");
        OrderResultDto mint = engine.submitOrder(CLOB_EVENT_ID, "Alice", YES, OrderSideDto.BUY, 40, 0.62);
        check("minted", mint.getFilledQuantity(), 35);
        check("remainder rests", mint.getRestingQuantity(), 5);
        check("it was a mint, not a trade", mint.getExecutions().get(0).isMint() ? 1 : 0, 1);
        // Carol rested first so her 0.42 stands; Alice pays the complement 0.58, under her 0.62 limit.
        check("Alice paid the complement, not her limit",
                mint.getExecutions().get(0).getPartyPrice(), 0.58);
        check("Carol was honoured at her own price",
                mint.getExecutions().get(0).getCounterpartyPrice(), 0.42);
        check("both payments went to the event account",
                accountOf(engine, CLOB_EVENT_ID), 135.00);
        check("Alice balance", balanceOf(engine, "Alice"), 164.85);
        check("Carol balance", balanceOf(engine, "Carol"), 180.31);
        checkConservation(engine, "after the mint", CLOB_TOTAL_CASH);

        // Step 18: Yes wins. 135 pairs exist, so 135 shares are paid out and the account empties.
        heading("Zoe closes the event, Yes wins");
        CloseResultDto close = engine.closeEvent(CLOB_EVENT_ID, "Zoe", YES);
        check("paid out to winners", close.getTotalPaidToWinners(), 135.00);
        check("event account emptied", accountOf(engine, CLOB_EVENT_ID), 0.00);

        heading("Final balances - these are the simulator's own numbers");
        check("Zoe", balanceOf(engine, "Zoe"), 486.31);
        check("Alice", balanceOf(engine, "Alice"), 224.85);
        check("Bob", balanceOf(engine, "Bob"), 198.54);
        check("Carol", balanceOf(engine, "Carol"), 190.31);
        checkConservation(engine, "after resolution", CLOB_TOTAL_CASH);
    }

    /** Submits an order expected to find no match and simply join the book. */
    private static void resting(GuessMarketEngine engine, String label, String user, int option,
                                OrderSideDto side, long quantity, double price, long expectedResting) {
        OrderResultDto result =
                engine.submitOrder(CLOB_EVENT_ID, user, option, side, quantity, price);
        check(label, result.getRestingQuantity(), expectedResting);
    }

    // ---------------------------------------------------------------- helpers

    private static void printOptionStates(GuessMarketEngine engine) {
        EventStateDto state = engine.getEventState(LMSR_EVENT_ID);
        for (OptionStateDto option : state.getOptionStates()) {
            System.out.printf("  %-12s price %.2f   shares %d%n",
                    option.getName(), option.getPrice(), option.getSharesBought());
        }
    }

    private static double priceOf(GuessMarketEngine engine, int zeroBasedIndex) {
        return engine.getEventState(LMSR_EVENT_ID).getOptionStates().get(zeroBasedIndex).getPrice();
    }

    private static double balanceOf(GuessMarketEngine engine, String userName) {
        return engine.getUser(userName).getBalance();
    }

    private static double accountOf(GuessMarketEngine engine, int eventId) {
        for (EventDto event : engine.getAllEvents()) {
            if (event.getId() == eventId) {
                return event.getAccountBalance();
            }
        }
        throw new IllegalStateException(
                "No event with id " + eventId + " - is small.xml really the file being loaded?");
    }

    /**
     * Money is never created or destroyed here: it only moves between user accounts and event
     * accounts. If this check ever fails, some transfer is crediting without debiting (or the
     * reverse), and every balance downstream of it is already wrong.
     */
    private static void checkConservation(GuessMarketEngine engine, String when) {
        checkConservation(engine, when, LMSR_TOTAL_CASH);
    }

    private static void checkConservation(GuessMarketEngine engine, String when, double expectedTotal) {
        double total = 0;
        for (UserDto user : engine.getAllUsers()) {
            total += user.getBalance();
        }
        for (EventDto event : engine.getAllEvents()) {
            total += event.getAccountBalance();
        }
        check("money conserved " + when, total, expectedTotal);
    }

    private static void heading(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    private static void check(String label, double actual, double expected) {
        checksRun++;
        boolean ok = Math.abs(actual - expected) < TOLERANCE;
        if (!ok) {
            checksFailed++;
        }
        System.out.printf("  [%s] %-38s expected %10.2f   got %10.2f%n",
                ok ? "OK  " : "FAIL", label, expected, actual);
    }
}
