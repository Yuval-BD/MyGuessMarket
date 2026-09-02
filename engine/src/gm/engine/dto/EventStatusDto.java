package gm.engine.dto;

/**
 * Mirrors the model's EventStatus. Deliberately a separate type so the UI layer never imports the
 * domain model, and so the two can diverge if the presentation ever needs a state the engine
 * does not have.
 */
public enum EventStatusDto {
    NOT_STARTED,
    ACTIVE,
    CLOSED
}
