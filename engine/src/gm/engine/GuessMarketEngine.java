package gm.engine;

import gm.engine.dto.CloseResultDto;
import gm.engine.dto.EventDto;
import gm.engine.dto.EventStateDto;
import gm.engine.dto.OrderBookStateDto;
import gm.engine.dto.OrderResultDto;
import gm.engine.dto.OrderSideDto;
import gm.engine.dto.PurchaseResultDto;
import gm.engine.dto.UserDto;
import gm.engine.dto.UserInvolvementDto;

import java.util.List;

/**
 * Everything a user interface may ask of the system. This interface plus the DTOs and the
 * exceptions are the entire surface the engine exposes.
 * <p>
 * The engine is passive: it answers questions and performs commands, and never calls out to
 * whoever is asking. There are no listeners, no callbacks and no JavaFX types here - a console
 * program, a desktop UI and, later, a server can all sit on top of exactly this.
 * <p>
 * Numbers cross this boundary as plain doubles. Rounding to two decimals is a presentation
 * decision and belongs to whoever is displaying them.
 */
public interface GuessMarketEngine {

    // ---------------------------------------------------------------- loading

    /**
     * Replaces the loaded system with the contents of the given file.
     * <p>
     * Either the whole file loads or nothing changes: on any problem this throws and the system
     * already loaded is left exactly as it was.
     */
    void loadEventsFromFile(String fullPath);

    boolean isFileLoaded();

    /** The path of the file currently loaded, or null if none has loaded successfully yet. */
    String getLoadedFilePath();

    // ---------------------------------------------------------------- users

    List<UserDto> getAllUsers();

    UserDto getUser(String userName);

    /**
     * Every event this user runs, has acted in, or may currently act in, with their holdings.
     * <p>
     * Derived by scanning the events rather than stored on the user: participations live on the
     * event, which is the single source of truth, and there are few enough events that scanning
     * costs nothing.
     */
    List<UserInvolvementDto> getUserInvolvements(String userName);

    // ---------------------------------------------------------------- events

    List<EventDto> getAllEvents();

    /** The LMSR trading state of one event: prices, shares, account, trade history. */
    EventStateDto getEventState(int eventId);

    /** The order-book state of one event: both books, their statistics, and the participants. */
    OrderBookStateDto getOrderBookState(int eventId);

    // ---------------------------------------------------------------- lifecycle

    /**
     * The market maker starts the event and pays the opening investment from his own balance.
     * Only that user may do this, and only while the event has not started.
     */
    void openEvent(int eventId, String marketMakerName);

    /**
     * The market maker decides the event, pays the winners, takes any on-close commission, and
     * for an LMSR event gets back whatever subsidy is left.
     */
    CloseResultDto closeEvent(int eventId, String marketMakerName, int optionNumber);

    // ---------------------------------------------------------------- trading

    /** Buys shares in an LMSR event on behalf of a user. Option numbers are 1-based. */
    PurchaseResultDto buyLmsrShares(int eventId, String userName, int optionNumber, long quantity);

    /**
     * Submits an order to an order-book event on behalf of a user. Option numbers are 1-based, and
     * the price is per share.
     */
    OrderResultDto submitOrder(int eventId, String userName, int optionNumber,
                               OrderSideDto side, long quantity, double pricePerShare);
}
