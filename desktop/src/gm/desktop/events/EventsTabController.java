package gm.desktop.events;

import gm.desktop.eventdetails.EventDetailsController;
import gm.engine.GuessMarketEngine;
import gm.engine.dto.CommissionTypeDto;
import gm.engine.dto.EventDto;
import gm.engine.dto.EventStatusDto;
import gm.engine.dto.TradingMethodTypeDto;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;

import java.util.ArrayList;
import java.util.List;

/**
 * The events table and its filters.
 * <p>
 * Filtering happens here rather than in the engine. Which toggles are pressed is view state, the
 * engine already hands over every field the filters need, and keeping the engine interface small
 * matters more once it becomes a network boundary.
 * <p>
 * The table is repopulated from DTOs on every refresh rather than bound to engine state - the engine
 * never pushes, so the UI pulls. {@link #refresh()} preserves the current selection by event id, so
 * a refresh after a trade does not make the selected event jump out from under the user.
 */
public class EventsTabController {

    @FXML private ToggleButton lmsrFilter;
    @FXML private ToggleButton orderBookFilter;
    @FXML private ToggleButton notStartedFilter;
    @FXML private ToggleButton activeFilter;
    @FXML private ToggleButton closedFilter;
    @FXML private ToggleButton onPurchaseFilter;
    @FXML private ToggleButton onCloseFilter;

    @FXML private Label eventCountLabel;

    /** Injected by the fx:include with fx:id "eventDetails". */
    @FXML private EventDetailsController eventDetailsController;

    @FXML private TableView<EventDto> eventsTable;
    @FXML private TableColumn<EventDto, String> nameColumn;
    @FXML private TableColumn<EventDto, String> statusColumn;
    @FXML private TableColumn<EventDto, String> methodColumn;
    @FXML private TableColumn<EventDto, String> commissionColumn;
    @FXML private TableColumn<EventDto, String> accountColumn;

    private final ObservableList<EventDto> rows = FXCollections.observableArrayList();

    private GuessMarketEngine engine;

    /**
     * Runs before the engine is injected, so it may only wire up controls - never ask for data.
     */
    @FXML
    private void initialize() {
        eventsTable.setItems(rows);

        nameColumn.setCellValueFactory(cell -> text(cell.getValue().getName()));
        statusColumn.setCellValueFactory(cell -> text(describe(cell.getValue().getStatus())));
        methodColumn.setCellValueFactory(cell -> text(describe(cell.getValue().getMethodType())));
        commissionColumn.setCellValueFactory(cell -> text(describeCommission(cell.getValue())));
        accountColumn.setCellValueFactory(cell -> text(money(cell.getValue().getAccountBalance())));

        for (ToggleButton filter : allFilters()) {
            filter.selectedProperty().addListener((_, _, _) -> refresh());
        }

        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((_, _, selected) -> onEventSelected(selected));
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        eventDetailsController.setEngine(engine);
    }

    /** Pulls the current events from the engine, applies the filters, and keeps the selection. */
    public void refresh() {
        if (engine == null || !engine.isFileLoaded()) {
            rows.clear();
            eventCountLabel.setText("");
            return;
        }

        Integer selectedId = selectedEventId();

        List<EventDto> allEvents = engine.getAllEvents();
        List<EventDto> visible = new ArrayList<>();
        for (EventDto event : allEvents) {
            if (matchesFilters(event)) {
                visible.add(event);
            }
        }
        rows.setAll(visible);

        eventCountLabel.setText(visible.size() == allEvents.size()
                ? String.format("%d events", allEvents.size())
                : String.format("%d of %d events", visible.size(), allEvents.size()));

        restoreSelection(selectedId);
        eventDetailsController.refresh();
    }

    @FXML
    private void onShowAllClicked() {
        for (ToggleButton filter : allFilters()) {
            filter.setSelected(true);        // each change fires refresh(); harmless and simple
        }
    }

    /** Same as on the users tab: a new file means the previous selection no longer refers to anything. */
    public void reset() {
        eventsTable.getSelectionModel().clearSelection();
        eventDetailsController.showEvent(null, null);
    }

    // ------------------------------------------------------------------ filtering

    private boolean matchesFilters(EventDto event) {
        return matchesMethod(event.getMethodType())
                && matchesStatus(event.getStatus())
                && matchesCommission(event.getCommissionType());
    }

    private boolean matchesMethod(TradingMethodTypeDto type) {
        return switch (type) {
            case LMSR -> lmsrFilter.isSelected();
            case ORDER_BOOK -> orderBookFilter.isSelected();
        };
    }

    private boolean matchesStatus(EventStatusDto status) {
        return switch (status) {
            case NOT_STARTED -> notStartedFilter.isSelected();
            case ACTIVE -> activeFilter.isSelected();
            case CLOSED -> closedFilter.isSelected();
        };
    }

    private boolean matchesCommission(CommissionTypeDto type) {
        return switch (type) {
            case ON_PURCHASE -> onPurchaseFilter.isSelected();
            case ON_CLOSE -> onCloseFilter.isSelected();
        };
    }

    private List<ToggleButton> allFilters() {
        return List.of(lmsrFilter, orderBookFilter,
                notStartedFilter, activeFilter, closedFilter,
                onPurchaseFilter, onCloseFilter);
    }

    // ------------------------------------------------------------------ selection

    private Integer selectedEventId() {
        EventDto selected = eventsTable.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    /**
     * Re-selects by id rather than by row index. The DTOs are rebuilt on every refresh, so the old
     * object is never the same instance, and rows can move as filters change.
     */
    private void restoreSelection(Integer eventId) {
        if (eventId == null) {
            return;
        }
        for (EventDto event : rows) {
            if (event.getId() == eventId) {
                eventsTable.getSelectionModel().select(event);
                return;
            }
        }
    }

    /** No acting user here: the events tab shows what happened, it is not where people act. */
    private void onEventSelected(EventDto event) {
        eventDetailsController.showEvent(event == null ? null : event.getId(), null);
    }

    // ------------------------------------------------------------------ formatting

    private static ObservableValue<String> text(String value) {
        return new ReadOnlyStringWrapper(value);
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

    private static String describeCommission(EventDto event) {
        String when = switch (event.getCommissionType()) {
            case ON_PURCHASE -> "on purchase";
            case ON_CLOSE -> "on close";
        };
        return String.format("%d%% %s", event.getCommissionPercent(), when);
    }

    private static String money(double amount) {
        return String.format("$%.2f", amount);
    }
}
