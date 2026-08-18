package gm.engine;

import gm.engine.dto.CloseResultDto;
import gm.engine.dto.EventDto;
import gm.engine.dto.EventStateDto;
import gm.engine.dto.PurchaseResultDto;

import java.util.List;

public interface GuessMarketEngine {

    void loadEventsFromFile(String fullPath);

    List<EventDto> getAllEvents();

    List<EventDto> getActiveEvents();

    EventStateDto getEventState(int eventNumber);

    PurchaseResultDto buyShares(int eventNumber, int optionNumber, long quantity);

    CloseResultDto closeEvent(int eventNumber, int optionNumber);
}
