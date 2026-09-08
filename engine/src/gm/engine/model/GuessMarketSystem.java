package gm.engine.model;

import gm.engine.exception.EventNotFoundException;
import gm.engine.exception.UserNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuessMarketSystem {

    private final List<Event> events;
    private final List<User> users;
    private final Map<Integer, Event> eventsById;
    private final Map<String, User> usersByName;

    public GuessMarketSystem(List<Event> events, List<User> users) {
        this.events = List.copyOf(events);
        this.users = List.copyOf(users);

        this.eventsById = new LinkedHashMap<>();
        for (Event event : this.events) {
            eventsById.put(event.getId(), event);
        }

        this.usersByName = new LinkedHashMap<>();
        for (User user : this.users) {
            usersByName.put(user.getName(), user);
        }
    }

    public List<Event> getEvents() {
        return events;
    }

    public List<User> getUsers() {
        return users;
    }

    public Event getEvent(int id) {
        Event event = eventsById.get(id);
        if (event == null) {
            throw new EventNotFoundException(String.format(
                    "Error: there is no event with id %d. Loaded events are: %s.",
                    id, listEventIds()));
        }
        return event;
    }

    public User getUser(String name) {
        User user = ((name == null) ? null : usersByName.get(name.trim()));
        if (user == null) {
            throw new UserNotFoundException(String.format(
                    "Error: there is no user named \"%s\". Loaded users are: %s.",
                    name, listUserNames()));
        }
        return user;
    }

    private String listEventIds() {
        return eventsById.keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private String listUserNames() {
        return String.join(", ", usersByName.keySet());
    }
}
