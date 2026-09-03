package gm.desktop.task;

import gm.engine.GuessMarketEngine;
import javafx.concurrent.Task;

import java.io.File;

/**
 * Loads an XML file into the engine off the JavaFX application thread.
 * <p>
 * This class lives in the desktop module rather than the engine, deliberately. A Task is soaked in
 * JavaFX concerns - it publishes to {@code messageProperty} and {@code progressProperty} so UI
 * controls can bind to it, and whoever writes one has to know which calls are safe off the FX
 * thread. Ask whether {@code progressProperty} would have meant anything to a console program: it
 * would not. The engine stays a plain object that knows nothing about who is calling it.
 * <p>
 * The real work here is fast - reading a small XML file - so the progress is simulated with short
 * sleeps around the actual call. The exercise asks for a visible progress indicator, and something
 * that flashes past in 8 milliseconds indicates nothing. Note that the engine is <em>not</em> given
 * a progress callback to make this honest: that would have the engine reporting outwards to its
 * caller, which is the inversion of control we are avoiding.
 */
public class LoadFileTask extends Task<Void> {

    private static final long PAUSE_BEFORE_MS = 500;
    private static final long PAUSE_AFTER_MS = 700;

    private final GuessMarketEngine engine;
    private final String fullPath;

    public LoadFileTask(GuessMarketEngine engine, String fullPath) {
        this.engine = engine;
        this.fullPath = fullPath;
    }

    @Override
    protected Void call() throws Exception {
        String fileName = new File(fullPath).getName();

        updateProgress(0, 1);
        updateMessage("Reading " + fileName + "...");
        Thread.sleep(PAUSE_BEFORE_MS);

        updateProgress(0.35, 1);
        updateMessage("Checking the file contents...");

        // Anything wrong with the file throws here. Task catches it, moves to the FAILED state and
        // hands it to setOnFailed, so the controller reports it without a try/catch of its own.
        engine.loadEventsFromFile(fullPath);

        updateProgress(0.8, 1);
        updateMessage("Preparing the system...");
        Thread.sleep(PAUSE_AFTER_MS);

        updateProgress(1, 1);
        updateMessage("Loaded " + fileName);
        return null;
    }
}
