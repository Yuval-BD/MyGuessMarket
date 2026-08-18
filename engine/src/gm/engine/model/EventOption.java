package gm.engine.model;

import gm.engine.exception.InvalidEventDataException;
import gm.engine.exception.InvalidQuantityException;
import java.util.Objects;

public class EventOption {

    private final String name;
    private long sharesBought = 0;

    public EventOption(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new InvalidEventDataException("Error: an option name must not be blank.");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public long getSharesBought() {
        return sharesBought;
    }

    public void addShares(long quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(
                    String.format("Error: quantity must be a positive number, but got %d.", quantity));
        }
        sharesBought += quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventOption other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}