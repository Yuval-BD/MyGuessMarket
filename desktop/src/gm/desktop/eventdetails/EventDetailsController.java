package gm.desktop.eventdetails;

import gm.desktop.orderbook.OrderBookViewController;
import gm.engine.GuessMarketEngine;
import gm.engine.dto.CloseResultDto;
import gm.engine.dto.EventDto;
import gm.engine.dto.EventStateDto;
import gm.engine.dto.EventStatusDto;
import gm.engine.dto.OptionStateDto;
import gm.engine.dto.OrderResultDto;
import gm.engine.dto.OrderSideDto;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.FlowPane;
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

    @FXML private VBox lmsrBox;
    @FXML private VBox orderBookBox;
    @FXML private FlowPane buyBar;
    @FXML private FlowPane orderBar;
    @FXML private ChoiceBox<String> orderSideChoice;
    @FXML private ChoiceBox<String> orderOptionChoice;
    @FXML private TextField orderQuantityField;
    @FXML private TextField orderPriceField;
    @FXML private Button submitOrderButton;

    /** Injected by the fx:include with fx:id "orderBookView". */
    @FXML private OrderBookViewController orderBookViewController;

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

        orderSideChoice.getItems().setAll("Buy", "Sell");
        orderSideChoice.setValue("Buy");

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
        // Only wipe the last result when the panel actually moves to a different event or a
        // different person. A refresh after an action re-selects the same row and lands here again;
        // clearing unconditionally would erase the message that action just produced.
        boolean sameTarget = java.util.Objects.equals(this.eventId, eventId)
                && java.util.Objects.equals(this.actingUserName, actingUserName);

        this.eventId = eventId;
        this.actingUserName = actingUserName;
        if (!sameTarget) {
            messageLabel.setText("");
            clearInputFields();
        }
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
            showOrderBook(event);
            return;
        }

        showLmsr();
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

    @FXML
    private void onSubmitOrderClicked() {
        Integer optionNumber = selectedOptionNumber(orderOptionChoice);
        if (optionNumber == null) {
            messageLabel.setText("Choose which option to trade first.");
            return;
        }

        long quantity;
        double price;
        try {
            quantity = Long.parseLong(orderQuantityField.getText().trim());
        } catch (NumberFormatException e) {
            messageLabel.setText("Quantity must be a whole number, for example 25.");
            return;
        }
        try {
            price = Double.parseDouble(orderPriceField.getText().trim());
        } catch (NumberFormatException e) {
            messageLabel.setText("Price must be a number, for example 0.58.");
            return;
        }
        if (quantity <= 0) {
            messageLabel.setText("Quantity must be greater than zero.");
            return;
        }

        OrderSideDto side = "Sell".equals(orderSideChoice.getValue())
                ? OrderSideDto.SELL : OrderSideDto.BUY;

        run(() -> {
            OrderResultDto result = engine.submitOrder(
                    eventId, actingUserName, optionNumber, side, quantity, price);
            orderQuantityField.clear();
            orderPriceField.clear();
            return describeOrderResult(result);
        });
    }

    /** Says what actually happened, since an order can partly fill, fully fill, mint, or just rest. */
    private String describeOrderResult(OrderResultDto result) {
        StringBuilder message = new StringBuilder();
        if (result.getFilledQuantity() > 0) {
            message.append(String.format("Executed %d of %d shares.",
                    result.getFilledQuantity(), result.getRequestedQuantity()));
            for (var execution : result.getExecutions()) {
                message.append(String.format("%n  %s %d at $%.2f with %s%s",
                        execution.isMint() ? "Minted" : "Traded",
                        execution.getQuantity(),
                        execution.getPartyPrice(),
                        execution.getCounterpartyName(),
                        execution.isMint()
                                ? String.format(" (who paid $%.2f for %s)",
                                        execution.getCounterpartyPrice(),
                                        execution.getCounterpartyOptionName())
                                : ""));
            }
        } else {
            message.append("Nothing matched.");
        }
        if (result.getRestingQuantity() > 0) {
            message.append(String.format("%n%d shares are resting in the book.",
                    result.getRestingQuantity()));
        }
        if (result.getTotalSpent() > 0) {
            message.append(String.format("%nSpent %s including %s commission.",
                    money(result.getTotalSpent()), money(result.getCommissionPaid())));
        }
        if (result.getTotalReceived() > 0) {
            message.append(String.format("%nReceived %s.", money(result.getTotalReceived())));
        }
        return message.toString();
    }

    /**
     * Runs an engine command, reports what happened, and refreshes. Every rule violation arrives
     * here as a GuessMarketException carrying a message written for the person reading it, so it is
     * shown as-is rather than translated.
     */
    private void run(EngineCommand command) {
        String message;
        try {
            message = command.execute();
        } catch (GuessMarketException e) {
            // Every rule violation arrives here carrying a message written for the person reading
            // it, so it is shown as-is rather than translated.
            message = e.getMessage();
        }

        refresh();
        onChanged.run();

        // Set the message last. The refresh above ripples out to the surrounding tabs, which
        // re-select rows and can re-enter this panel - anything written before that can be lost.
        messageLabel.setText(message);
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

        boolean isOrderBook = event.getMethodType() == TradingMethodTypeDto.ORDER_BOOK;
        setSectionVisible(buyBar, !isOrderBook);
        setSectionVisible(orderBar, isOrderBook);

        buyOptionChoice.setDisable(!active);
        quantityField.setDisable(!active);
        buyButton.setDisable(!active);

        orderSideChoice.setDisable(!active);
        orderOptionChoice.setDisable(!active);
        orderQuantityField.setDisable(!active);
        orderPriceField.setDisable(!active);
        submitOrderButton.setDisable(!active);

        // The winner choice always needs the option names; the LMSR buy row only exists for LMSR
        // events, and the order row takes its names from the event rather than from price state.
        fillOptionChoiceFromNames(winnerChoice, event.getOptionNames());
        fillOptionChoiceFromNames(orderOptionChoice, event.getOptionNames());
        if (!isOrderBook) {
            fillOptionChoice(buyOptionChoice, options);
        }
    }

    private void fillOptionChoiceFromNames(ChoiceBox<String> choice, List<String> names) {
        String previous = choice.getValue();
        choice.getItems().setAll(names);
        if (previous != null && names.contains(previous)) {
            choice.setValue(previous);
        } else if (!names.isEmpty()) {
            choice.setValue(names.get(0));
        }
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

    /**
     * Empties the typed-in fields. A quantity or price belongs to the event and the person it was
     * typed for, so it is wiped when the panel moves to a different one - otherwise a number left
     * over from an earlier event sits there looking like a suggestion.
     */
    private void clearInputFields() {
        quantityField.clear();
        orderQuantityField.clear();
        orderPriceField.clear();
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

    /** Order book events show two books and their participants instead of a price curve. */
    private void showOrderBook(EventDto event) {
        setSectionVisible(lmsrBox, false);
        setSectionVisible(orderBookBox, true);
        optionRows.clear();
        tradeRows.clear();

        orderBookViewController.show(engine.getOrderBookState(eventId));

        accountLabel.setText("Event account: " + money(event.getAccountBalance()));
        commissionLabel.setText("");
        winnerLabel.setText(event.getWinningOptionName() == null
                ? "" : "Winning option: " + event.getWinningOptionName());

        updateActionControls(event, List.of());
    }

    private void showLmsr() {
        setSectionVisible(lmsrBox, true);
        setSectionVisible(orderBookBox, false);
        orderBookViewController.show(null);
    }

    private static void setSectionVisible(javafx.scene.layout.Pane box, boolean visible) {
        box.setVisible(visible);
        box.setManaged(visible);
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
