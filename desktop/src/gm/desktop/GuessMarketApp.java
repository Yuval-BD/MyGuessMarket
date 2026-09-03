package gm.desktop;

import gm.desktop.app.AppController;
import gm.engine.GuessMarketEngine;
import gm.engine.GuessMarketEngineImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * The JavaFX application, and the composition root of the desktop module: the one place that names
 * a concrete engine implementation. Everything below here works against the GuessMarketEngine
 * interface, which is what lets the same engine sit behind the smoke test and, later, a server.
 */
public class GuessMarketApp extends Application {

    private static final String APP_FXML = "/gm/desktop/app/app.fxml";
    private static final String TITLE = "Guess Market";

    private static final double INITIAL_WIDTH = 1100;
    private static final double INITIAL_HEIGHT = 700;
    private static final double MIN_WIDTH = 640;
    private static final double MIN_HEIGHT = 480;

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlLocation = getClass().getResource(APP_FXML);
        if (fxmlLocation == null) {
            throw new IllegalStateException(
                    "Could not find " + APP_FXML + " on the classpath. "
                            + "Check that IntelliJ is copying *.fxml files into the module output.");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        GuessMarketEngine engine = new GuessMarketEngineImpl();
        AppController controller = loader.getController();
        controller.setEngine(engine);
        controller.setOwnerWindow(stage);

        stage.setTitle(TITLE);
        stage.setScene(new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT));
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }
}
