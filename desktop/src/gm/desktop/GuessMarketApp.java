package gm.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * The JavaFX application. Owns the primary stage and loads the root FXML.
 */
public class GuessMarketApp extends Application {

    private static final String APP_FXML = "/gm/desktop/app/app.fxml";
    private static final String TITLE = "Guess Market";

    private static final double INITIAL_WIDTH = 1000;
    private static final double INITIAL_HEIGHT = 650;
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

        Parent root = FXMLLoader.load(fxmlLocation);

        stage.setTitle(TITLE);
        stage.setScene(new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT));
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.show();
    }
}
