package gm.desktop.eventdetails;

import gm.engine.GuessMarketEngine;
import gm.engine.dto.CloseResultDto;
import gm.engine.dto.EventDto;
import gm.engine.dto.EventStateDto;
import gm.engine.dto.EventStatusDto;
import gm.engine.dto.OptionStateDto;
import gm.engine.dto.PurchaseResultDto;
import gm.engine.dto.TradeDto;
import gm.engine.dto.TradingMethodTypeDto;
import gm.engine.exception.GuessMarketException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Shows one event, and - when an acting user is supplied - lets that user act on it.
 * <p>
 * The same panel serves both tabs. The events tab passes no acting user, so it is purely
 * informational; the users tab passes the selected user, which reveals the controls that user is
 * allowed to use. That split follows the exercise: participation is driven from the users area, and
 * the events area reflects the results.
 * <p>
 * Whether a button is enabled here is a convenience, never a guarantee. The engine re-checks every
 * rule - the panel can be showing state from a moment ago.
 */
public class EventDetailsController {

    private static final String NOT_AVAILABLE = "-";

    @FXML private VBox root;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label descriptionLabel;

    @FXML private VBox actionBox;
    @FXML private Button openButton;
    @FXML private Button closeButton;
    @FXML private ChoiceBox<String> winnerChoice;
    @FXML private ChoiceBox<String> buyOptionChoice;
    @FXML private TextField quantityField;
    @FXML private Button buyButton;
    @FXML private Label messageLabel;

    @FXML private TableView<OptionStateDto> optionsTable;
    @FXML private TableColumn<OptionStateDto, String> optionNameColumn;
    @FXML private TableColumn<OptionStateDto, String> optionPriceColumn;
    @FXML private TableColumn<OptionStateDto, String> optionSharesColumn;

    @FXML private Label accountLabel;
    @FXML private Label commissionLabel;
    @FXML private Label winnerLabel;

    @FXML private TableView<TradeDto> tradesTable;
    @FXML private TableColumn<TradeDto, String> tradeBuyerColumn;
    @FXML private TableColumn<TradeDto, String> tradeOptionColumn;
    @FXML private TableColumn<TradeDto, String> tradeQuantityColumn;
    @FXML private TableColumn<TradeDto, String> tradeCostColumn;
    @FXML private TableColumn<TradeDto, String> tradeCommissionColumn;
    @FXML private TableColumn<TradeDto, String> tradeTotalColumn;

    private final ObservableList<OptionStateDto> optionRows = FXCollections.observableArrayList();
    private final ObservableList<TradeDto> tradeRows = FXCollections.observableArrayList();

    private GuessMarketEngine engine;
    private Runnable onChanged = () -> { };

    private Integer eventId;
    private String actingUserName;

    @FXML
    private void initialize() {
        optionsTable.setItems(optionRows);
        tradesTable.setItems(tradeRows);

        optionNameColumn.setCellValueFactory(cell -> text(cell.getValue().getName()));
        optionPriceColumn.setCellValueFactory(cell -> text(price(cell.getValue().getPrice())));
        optionSharesColumn.setCellValueFactory(cell -> text(String.valueOf(cell.getValue().getSharesBought())));

        tradeBuyerColumn.setCellValueFactory(cell -> text(cell.getValue().getBuyerName()));
        tradeOptionColumn.setCellValueFactory(cell -> text(cell.getValue().getOptionName()));
        tradeQuantityColumn.setCellValueFactory(cell -> text(String.valueOf(cell.getValue().getQuantity())));
        tradeCostColumn.setCellValueFactory(cell -> text(money(cell.getValue().getSharesCost())));
        tradeCommissionColumn.setCellValueFactory(cell -> text(money(cell.getValue().getCommissionPaid())));
        tradeTotalColumn.setCellValueFactory(cell -> text(money(cell.getValue().getTotalPaid())));

        showNothing();
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    /** Called after any action so the surrounding tabs can refresh their own tables too. */
    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged == null ? () -> { } : onChanged;
    }

    /**
     * @param eventId        the event to show, or null to show nothing
     * @param actingUserName the user acting here, or null for a read-only view
     */
    public void showEvent(Integer eventId, String actingUserName) {
        this.eventId = eventId;
        this.actingUserName = actingUserName;
        messageLabel.setText("");
        refresh();
    }

    public void refresh() {
        if (engine == null || eventId == null || !engine.isFileLoaded()) {
            showNothing();
            return;
        }

        EventDto event = findEvent(eventId);
        if (event == null) {
            showNothing();
            return;
        }

        titleLabel.setText(event.getName());
        subtitleLabel.setText(String.format("%s  |  %s  |  market maker: %s  |  %d%% %s",
                describe(event.getStatus()),
                describe(event.getMethodType()),
                event.getMarketMakerName(),
                event.getCommissionPercent(),
                event.getCommissionType() == gm.engine.dto.CommissionTypeDto.ON_PURCHASE
                        ? "on purchase" : "on close"));
        descriptionLabel.setText(event.getDescription());

        if (event.getMethodType() == TradingMethodTypeDto.ORDER_BOOK) {
            showOrderBookNotYetSupported(event);
            return;
        }

        EventStateDto state = engine.getEventState(eventId);
        optionRows.setAll(state.getOptionStates());
        tradeRows.setAll(state.getTrades());

        accountLabel.setText("Event account: " + money(state.getAccountBalance()));
        commissionLabel.setText("Commission generated for the market maker: "
                + money(state.getCommissionCollected()));
        winnerLabel.setText(state.getWinningOptionName() == null
                ? ""
                : "Winning option: " + state.getWinningOptionName());

        updateActionControls(event, state.getOptionStates());
    }

    // ------------------------------------------------------------------ actions

    @FXML
    private void onOpenClicked() {
        run(() -> {
            engine.openEvent(eventId, actingUserName);
            return String.format("%s opened the event.", actingUserName);
        });
    }

    @FXML
    private void onCloseClicked() {
        Integer optionNumber = selectedOptionNumber(winnerChoice);
        if (optionNumber == null) {
            messageLabel.setText("Choose the winning option first.");
            return;
        }
        run(() -> {
            CloseResultDto result = engine.closeEvent(eventId, actingUserName, optionNumber);
            return String.format(
                    "Closed. \"%s\" won. Paid %s to winners, commission %s, %s returned to %s.",
                    result.getWinningOptionName(),
                    money(result.getTotalPaidToWinners()),
                    money(result.getTotalCommissionCollected()),
                    money(result.getLeftoverReturnedToMarketMaker()),
                    actingUserName);
        });
    }

    @FXML
    private void onBuyClicked() {
        Integer optionNumber = selectedOptionNumber(buyOptionChoice);
        if (optionNumber == null) {
            messageLabel.setText("Choose an option to buy first.");
            return;
        }

        long quantity;
        try {
            quantity = Long.parseLong(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            messageLabel.setText("Quantity must be a whole number, for example 100.");
            return;
        }
        if (quantity <= 0) {
            messageLabel.setText("Quantity must be greater than zero.");
            return;
        }

        run(() -> {
            PurchaseResultDto result =
                    engine.buyLmsrShares(eventId, actingUserName, optionNumber, quantity);
            quantityField.clear();
            return String.format("Bought %d of \"%s\" for %s (shares %s + commission %s). %s now has %s.",
                    result.getQuantity(), result.getOptionName(), money(result.getTotalPaid()),
                    money(result.getSharesCost()), money(result.getCommissionPaid()),
                    result.getBuyerName(), money(result.getBuyerBalanceAfter()));
        });
    }

    /**
     * Runs an engine command, reports what happened, and refreshes. Every rule violation arrives
     * here as a GuessMarketException carrying a message written for the person reading it, so it is
     * shown as-is rather than translated.
     */
    private void run(EngineCommand command) {
        try {
            messageLabel.setText(command.execute());
        } catch (GuessMarketException e) {
            messageLabel.setText(e.getMessage());
        }
        refresh();
        onChanged.run();
    }

    @FunctionalInterface
    private interface EngineCommand {
        String execute();
    }

    // ------------------------------------------------------------------ view state

    private void updateActionControls(EventDto event, List<OptionStateDto> options) {
        boolean canAct = actingUserName != null;
        actionBox.setVisible(canAct);
        actionBox.setManaged(canAct);
        if (!canAct) {
            return;
        }

        boolean isMarketMaker = actingUserName.equals(event.getMarketMakerName());
        boolean notStarted = event.getStatus() == EventStatusDto.NOT_STARTED;
        boolean active = event.getStatus() == EventStatusDto.ACTIVE;

        openButton.setDisable(!isMarketMaker || !notStarted);
        closeButton.setDisable(!isMarketMaker || !active);
        winnerChoice.setDisable(!isMarketMaker || !active);

        buyOptionChoice.setDisable(!active);
        quantityField.setDisable(!active);
        buyButton.setDisable(!active);

        fillOptionChoice(winnerChoice, options);
        fillOptionChoice(buyOptionChoice, options);
    }

    /** Keeps the current pick if it is still one of the options, so a refresh does not reset it. */
    private void fillOptionChoice(ChoiceBox<String> choice, List<OptionStateDto> options) {
        String previous = choice.getValue();
        List<String> names = options.stream().map(OptionStateDto::getName).toList();
        choice.getItems().setAll(names);
        if (previous != null && names.contains(previous)) {
            choice.setValue(previous);
        } else if (!names.isEmpty()) {
            choice.setValue(names.get(0));
        }
    }

    /** Option numbers are 1-based everywhere the engine is concerned. */
    private Integer selectedOptionNumber(ChoiceBox<String> choice) {
        int index = choice.getItems().indexOf(choice.getValue());
        return index < 0 ? null : index + 1;
    }

    private void showNothing() {
        titleLabel.setText("Select an event to see its details.");
        subtitleLabel.setText("");
        descriptionLabel.setText("");
        accountLabel.setText("");
        commissionLabel.setText("");
        winnerLabel.setText("");
        optionRows.clear();
        tradeRows.clear();
        actionBox.setVisible(false);
        actionBox.setManaged(false);
    }

    private void showOrderBookNotYetSupported(EventDto event) {
        optionRows.clear();
        tradeRows.clear();
        accountLabel.setText("Event account: " + money(event.getAccountBalance()));
        commissionLabel.setText("");
        winnerLabel.setText("The order book view arrives in a later phase.");
        actionBox.setVisible(false);
        actionBox.setManaged(false);
    }

    private EventDto findEvent(int id) {
        for (EventDto event : engine.getAllEvents()) {
            if (event.getId() == id) {
                return event;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ formatting

    private static ObservableValue<String> text(String value) {
        return new ReadOnlyStringWrapper(value);
    }

    private static String money(double amount) {
        return String.format("$%.2f", amount);
    }

    private static String price(double value) {
        return String.format("%.2f", value);
    }

    private static String describe(EventStatusDto status) {
        return switch (status) {
            case NOT_STARTED -> "Not started";
            case ACTIVE -> "Active";
            case CLOSED -> "Closed";
        };
    }

    private static String describe(TradingMethodTypeDto type) {
        return switch (type) {
            case LMSR -> "LMSR";
            case ORDER_BOOK -> "Order Book";
        };
    }
}
