package gm.desktop.orderbook;

import gm.engine.dto.BookStatsDto;
import gm.engine.dto.HoldingDto;
import gm.engine.dto.OptionBookDto;
import gm.engine.dto.OrderBookStateDto;
import gm.engine.dto.ParticipantDto;
import gm.engine.dto.RestingOrderDto;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws both order books of one event, their price indicators, and everyone taking part.
 * <p>
 * Purely a view: it is handed a state snapshot and renders it. It never calls the engine, so the
 * details panel around it stays the single place that decides when to refresh.
 */
public class OrderBookViewController {

    /** Shown wherever a statistic has no value yet - never 0.00, which would read as a real price. */
    private static final String NO_VALUE = "—";

    @FXML private Label mintLabel;

    @FXML private Label firstOptionLabel;
    @FXML private Label firstStatsLabel;
    @FXML private TableView<RestingOrderDto> firstBidsTable;
    @FXML private TableColumn<RestingOrderDto, String> firstBidUserColumn;
    @FXML private TableColumn<RestingOrderDto, String> firstBidQtyColumn;
    @FXML private TableColumn<RestingOrderDto, String> firstBidPriceColumn;
    @FXML private TableView<RestingOrderDto> firstAsksTable;
    @FXML private TableColumn<RestingOrderDto, String> firstAskUserColumn;
    @FXML private TableColumn<RestingOrderDto, String> firstAskQtyColumn;
    @FXML private TableColumn<RestingOrderDto, String> firstAskPriceColumn;

    @FXML private Label secondOptionLabel;
    @FXML private Label secondStatsLabel;
    @FXML private TableView<RestingOrderDto> secondBidsTable;
    @FXML private TableColumn<RestingOrderDto, String> secondBidUserColumn;
    @FXML private TableColumn<RestingOrderDto, String> secondBidQtyColumn;
    @FXML private TableColumn<RestingOrderDto, String> secondBidPriceColumn;
    @FXML private TableView<RestingOrderDto> secondAsksTable;
    @FXML private TableColumn<RestingOrderDto, String> secondAskUserColumn;
    @FXML private TableColumn<RestingOrderDto, String> secondAskQtyColumn;
    @FXML private TableColumn<RestingOrderDto, String> secondAskPriceColumn;

    @FXML private TableView<ParticipantDto> participantsTable;
    @FXML private TableColumn<ParticipantDto, String> participantNameColumn;
    @FXML private TableColumn<ParticipantDto, String> participantHoldingsColumn;
    @FXML private TableColumn<ParticipantDto, String> participantValueColumn;
    @FXML private TableColumn<ParticipantDto, String> participantCommissionColumn;

    private final ObservableList<RestingOrderDto> firstBids = FXCollections.observableArrayList();
    private final ObservableList<RestingOrderDto> firstAsks = FXCollections.observableArrayList();
    private final ObservableList<RestingOrderDto> secondBids = FXCollections.observableArrayList();
    private final ObservableList<RestingOrderDto> secondAsks = FXCollections.observableArrayList();
    private final ObservableList<ParticipantDto> participants = FXCollections.observableArrayList();

    private int baseValue = 1;

    @FXML
    private void initialize() {
        firstBidsTable.setItems(firstBids);
        firstAsksTable.setItems(firstAsks);
        secondBidsTable.setItems(secondBids);
        secondAsksTable.setItems(secondAsks);
        participantsTable.setItems(participants);

        wireOrderColumns(firstBidUserColumn, firstBidQtyColumn, firstBidPriceColumn);
        wireOrderColumns(firstAskUserColumn, firstAskQtyColumn, firstAskPriceColumn);
        wireOrderColumns(secondBidUserColumn, secondBidQtyColumn, secondBidPriceColumn);
        wireOrderColumns(secondAskUserColumn, secondAskQtyColumn, secondAskPriceColumn);

        participantNameColumn.setCellValueFactory(cell -> text(
                cell.getValue().getUserName() + (cell.getValue().isMarketMaker() ? " (MM)" : "")));
        participantHoldingsColumn.setCellValueFactory(cell -> text(describeHoldings(cell.getValue())));
        participantValueColumn.setCellValueFactory(cell -> text(describeBestCase(cell.getValue())));
        participantCommissionColumn.setCellValueFactory(
                cell -> text(money(cell.getValue().getTotalCommissionPaid())));
    }

    /** Renders a snapshot. Passing null clears the view. */
    public void show(OrderBookStateDto state) {
        if (state == null) {
            clear();
            return;
        }

        baseValue = state.getBaseValue();
        mintLabel.setText(String.format(
                "Base value $%d per winning share.  %s",
                state.getBaseValue(),
                state.isAllowMint()
                        ? "Minting is on: two buy orders whose prices together reach the base value "
                                + "create new shares."
                        : "Minting is off: shares can only change hands, never be created."));

        List<OptionBookDto> books = state.getBooks();
        showBook(books.isEmpty() ? null : books.getFirst(),
                firstOptionLabel, firstStatsLabel, firstBids, firstAsks);
        showBook(books.size() < 2 ? null : books.get(1),
                secondOptionLabel, secondStatsLabel, secondBids, secondAsks);

        participants.setAll(state.getParticipants());
    }

    private void showBook(OptionBookDto book, Label optionLabel, Label statsLabel,
                          ObservableList<RestingOrderDto> bids, ObservableList<RestingOrderDto> asks) {
        if (book == null) {
            optionLabel.setText("");
            statsLabel.setText("");
            bids.clear();
            asks.clear();
            return;
        }

        optionLabel.setText(String.format("%s  (%d shares in existence)",
                book.getOptionName(), book.getSharesOutstanding()));
        statsLabel.setText(describeStats(book.getStats()));
        bids.setAll(book.getBids());
        asks.setAll(book.getAsks());
    }

    private void clear() {
        mintLabel.setText("");
        showBook(null, firstOptionLabel, firstStatsLabel, firstBids, firstAsks);
        showBook(null, secondOptionLabel, secondStatsLabel, secondBids, secondAsks);
        participants.clear();
    }

    // ------------------------------------------------------------------ formatting

    private void wireOrderColumns(TableColumn<RestingOrderDto, String> userColumn,
                                  TableColumn<RestingOrderDto, String> quantityColumn,
                                  TableColumn<RestingOrderDto, String> priceColumn) {
        userColumn.setCellValueFactory(cell -> text(cell.getValue().getUserName()));
        quantityColumn.setCellValueFactory(cell -> text(String.valueOf(cell.getValue().getQuantity())));
        priceColumn.setCellValueFactory(cell -> text(money(cell.getValue().getPricePerShare())));
    }

    private static String describeStats(BookStatsDto stats) {
        return String.format("Last %s   Bid %s   Ask %s   Mid %s   Spread %s",
                price(stats.getLastTradePrice()),
                price(stats.getBestBid()),
                price(stats.getBestAsk()),
                price(stats.getMid()),
                price(stats.getSpread()));
    }

    private static String describeHoldings(ParticipantDto participant) {
        List<String> parts = new ArrayList<>();
        for (HoldingDto holding : participant.getHoldings()) {
            if (holding.getShares() > 0) {
                parts.add(String.format("%s x%d (paid %s)",
                        holding.getOptionName(), holding.getShares(), money(holding.getAmountPaid())));
            }
        }
        return parts.isEmpty() ? NO_VALUE : String.join(", ", parts);
    }

    /**
     * The most this participant could receive: every share they hold paying out in full. Only one
     * option can win, so this is a ceiling rather than a valuation.
     */
    private String describeBestCase(ParticipantDto participant) {
        long best = 0;
        for (HoldingDto holding : participant.getHoldings()) {
            best = Math.max(best, holding.getShares());
        }
        return best == 0 ? NO_VALUE : money(best * (double) baseValue);
    }

    private static ObservableValue<String> text(String value) {
        return new ReadOnlyStringWrapper(value);
    }

    private static String money(double amount) {
        return String.format("$%.2f", amount);
    }

    /** Null means the number does not exist yet, which is shown as a dash, never as zero. */
    private static String price(Double value) {
        return value == null ? NO_VALUE : String.format("$%.2f", value);
    }
}
