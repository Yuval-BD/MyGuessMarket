package gm.desktop.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller for the application shell: the top bar and, later, the tab pane.
 */
public class AppController {

    @FXML
    private TextField loadedFilePathField;

    @FXML
    private Label statusLabel;

    /**
     * Called by the FXMLLoader once every {@code @FXML} field has been injected.
     * Note that it runs after injection, so the fields are safe to touch here but
     * would be null in a constructor.
     */
    @FXML
    private void initialize() {
        statusLabel.setText("JavaFX is running. No file loaded yet.");
    }

    @FXML
    private void onLoadFileClicked() {
        loadedFilePathField.setText("(not wired yet)");
        statusLabel.setText("Load File clicked - the FileChooser arrives in Phase 4.");
    }
}
