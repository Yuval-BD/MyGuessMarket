package gm.engine.model;

import gm.engine.exception.EventNotFoundException;
import java.util.List;

public class GuessMarketSystem {

    private final List<Event> events;

    public GuessMarketSystem(List<Event> events) {
        this.events = List.copyOf(events);
    }

    public List<Event> getEvents() {
        return events;
    }

    public Event getEvent(int eventNumber) {
        if (eventNumber < 1 || eventNumber > events.size()) {
            throw new EventNotFoundException(String.format(
                    "Error: there is no event numbered %d. Valid range is 1 to %d.", eventNumber, events.size()));
        }
        return events.get(eventNumber - 1);
    }
}