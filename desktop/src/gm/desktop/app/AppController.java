package gm.desktop.app;

import gm.desktop.events.EventsTabController;
import gm.desktop.users.UsersTabController;
import gm.desktop.task.LoadFileTask;
import gm.engine.GuessMarketEngine;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/**
 * The application shell: the load bar across the top and the tabs below it.
 * <p>
 * Nothing here is bound to engine state. The engine exposes plain data and never pushes, so this
 * controller pulls: after any operation that could have changed something, it calls a refresh. That
 * discipline is established here, while there is one controller, rather than retrofitted across ten.
 */
public class AppController {

    private static final String XML_DESCRIPTION = "Guess Market XML files";
    private static final String XML_GLOB = "*.xml";

    @FXML private Button loadFileButton;
    @FXML private TextField loadedFilePathField;
    @FXML private HBox progressBox;
    @FXML private ProgressBar loadProgressBar;
    @FXML private Label loadStatusLabel;
    @FXML private TabPane mainTabPane;

    /**
     * Injected by the FXMLLoader: an fx:include with fx:id "eventsView" also publishes its
     * controller as "eventsViewController".
     */
    @FXML private EventsTabController eventsViewController;

    /** Same convention: the fx:include with fx:id "usersView" publishes "usersViewController". */
    @FXML private UsersTabController usersViewController;

    private GuessMarketEngine engine;
    private Window ownerWindow;
    private File lastFolder;

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        eventsViewController.setEngine(engine);
        usersViewController.setEngine(engine);
        // Trading happens on the users tab, but it changes event state and account balances that
        // the events tab shows, so anything done there refreshes the events side too.
        usersViewController.setOnChanged(eventsViewController::refresh);
    }

    /** Needed so the file chooser opens as a modal child of the main window. */
    public void setOwnerWindow(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
    }

    @FXML
    private void onLoadFileClicked() {
        File chosen = chooseFile();
        if (chosen == null) {
            return;                       // the user cancelled - not an error, say nothing
        }
        lastFolder = chosen.getParentFile();
        startLoad(chosen.getAbsolutePath());
    }

    private File chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a Guess Market XML file");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(XML_DESCRIPTION, XML_GLOB));
        if (lastFolder != null && lastFolder.isDirectory()) {
            chooser.setInitialDirectory(lastFolder);
        }
        return chooser.showOpenDialog(ownerWindow);
    }

    private void startLoad(String fullPath) {
        LoadFileTask task = new LoadFileTask(engine, fullPath);

        loadProgressBar.progressProperty().bind(task.progressProperty());
        loadStatusLabel.textProperty().bind(task.messageProperty());
        setLoadingVisible(true);
        loadFileButton.setDisable(true);

        task.setOnSucceeded(event -> {
            finishLoad(task);
            loadedFilePathField.setText(engine.getLoadedFilePath());
            mainTabPane.setDisable(false);
            refreshAll();
        });

        task.setOnFailed(event -> {
            finishLoad(task);
            // A failed load leaves the previously loaded system untouched, so nothing is refreshed
            // and nothing on screen changes except the message.
            showLoadError(task.getException());
        });

        Thread thread = new Thread(task, "guess-market-load");
        thread.setDaemon(true);           // must not keep the JVM alive after the window closes
        thread.start();
    }

    private void finishLoad(LoadFileTask task) {
        // The bindings have to go before anything sets these controls directly - a bound property
        // throws if you assign to it.
        loadProgressBar.progressProperty().unbind();
        loadStatusLabel.textProperty().unbind();
        loadProgressBar.setProgress(0);
        loadStatusLabel.setText("");
        setLoadingVisible(false);
        loadFileButton.setDisable(false);
    }

    private void setLoadingVisible(boolean visible) {
        progressBox.setVisible(visible);
        progressBox.setManaged(visible);   // managed=false so it takes no layout space when hidden
    }

    /**
     * Pulls fresh data from the engine into every region that could have changed. Called after any
     * successful operation. Right now that is two labels; as tabs arrive, each gets its own refresh
     * method called from here.
     */
    private void refreshAll() {
        eventsViewController.refresh();
        usersViewController.refresh();
    }

    private void showLoadError(Throwable failure) {
        String message = failure == null ? "Unknown error." : failure.getMessage();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(ownerWindow);
        alert.setTitle("The file could not be loaded");
        alert.setHeaderText("The file was not loaded. The system you had loaded is unchanged.");

        // A content problem lists every fault in the file, which can be many lines - a plain
        // content label would clip them.
        TextArea details = new TextArea(message);
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(8);
        alert.getDialogPane().setContent(details);
        alert.getDialogPane().setPrefWidth(640);

        alert.showAndWait();
    }
}
