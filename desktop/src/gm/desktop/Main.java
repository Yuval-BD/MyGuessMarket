package gm.desktop;

import javafx.application.Application;

/**
 * Process entry point.
 * <p>
 * Deliberately does NOT extend {@link Application}. When the main class of a jar extends
 * Application, the JVM launcher insists that the JavaFX runtime components are present as
 * named modules before {@code main} is even reached, which makes packaging fragile. A plain
 * launcher class sidesteps that entirely and costs nothing.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Application.launch(GuessMarketApp.class, args);
    }
}
