package gm.engine;

import gm.engine.dto.CloseResultDto;
import gm.engine.dto.DtoMapper;
import gm.engine.dto.EventDto;
import gm.engine.dto.EventStateDto;
import gm.engine.dto.PurchaseResultDto;
import gm.engine.dto.UserDto;
import gm.engine.exception.NoFileLoadedException;
import gm.engine.model.ClosingOutcome;
import gm.engine.model.Event;
import gm.engine.model.EventOption;
import gm.engine.model.GuessMarketSystem;
import gm.engine.model.Trade;
import gm.engine.model.User;
import gm.engine.xml.XmlEventLoader;

import java.util.List;

/**
 * Thin coordinator. It holds the loaded system, resolves the keys the UI speaks in (event id,
 * user name, 1-based option number) into model objects, and converts what comes back into DTOs.
 * <p>
 * Deliberately thin: the rules live in the model. If logic starts collecting here, it usually
 * belongs on {@link Event} or {@link User} instead.
 */
public class GuessMarketEngineImpl implements GuessMarketEngine {

    private final XmlEventLoader loader = new XmlEventLoader();

    private GuessMarketSystem system;
    private String loadedFilePath;

    @Override
    public void loadEventsFromFile(String fullPath) {
        // Assigned only on success, so a failed load leaves the previous system untouched.
        GuessMarketSystem loaded = loader.load(fullPath);
        system = loaded;
        loadedFilePath = fullPath.trim();
    }

    @Override
    public boolean isFileLoaded() {
        return system != null;
    }

    @Override
    public String getLoadedFilePath() {
        return loadedFilePath;
    }

    @Override
    public List<UserDto> getAllUsers() {
        return DtoMapper.toUserDtos(requireSystem().getUsers());
    }

    @Override
    public UserDto getUser(String userName) {
        return DtoMapper.toUserDto(requireSystem().getUser(userName));
    }

    @Override
    public List<EventDto> getAllEvents() {
        return DtoMapper.toEventDtos(requireSystem().getEvents());
    }

    @Override
    public EventStateDto getEventState(int eventId) {
        return DtoMapper.toEventStateDto(requireSystem().getEvent(eventId));
    }

    @Override
    public void openEvent(int eventId, String marketMakerName) {
        GuessMarketSystem loaded = requireSystem();
        Event event = loaded.getEvent(eventId);
        User actor = loaded.getUser(marketMakerName);
        event.open(actor);
    }

    @Override
    public CloseResultDto closeEvent(int eventId, String marketMakerName, int optionNumber) {
        GuessMarketSystem loaded = requireSystem();
        Event event = loaded.getEvent(eventId);
        User actor = loaded.getUser(marketMakerName);
        EventOption winner = event.getOption(optionNumber);

        ClosingOutcome outcome = event.close(actor, winner);
        return DtoMapper.toCloseResultDto(outcome, event);
    }

    @Override
    public PurchaseResultDto buyLmsrShares(int eventId, String userName, int optionNumber, long quantity) {
        GuessMarketSystem loaded = requireSystem();
        Event event = loaded.getEvent(eventId);
        User buyer = loaded.getUser(userName);

        Trade trade = event.buyLmsr(buyer, optionNumber, quantity);
        return DtoMapper.toPurchaseResultDto(trade, buyer, event);
    }

    private GuessMarketSystem requireSystem() {
        if (system == null) {
            throw new NoFileLoadedException(
                    "Error: no file is loaded yet. Load a Guess Market XML file first.");
        }
        return system;
    }
}
