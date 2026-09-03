package gm.desktop.users;

import gm.desktop.eventdetails.EventDetailsController;
import gm.engine.GuessMarketEngine;
import gm.engine.dto.EventStatusDto;
import gm.engine.dto.HoldingDto;
import gm.engine.dto.UserDto;
import gm.engine.dto.UserInvolvementDto;
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
 * The users tab: who is in the system, what each of them is involved in, and - through the shared
 * details panel - the place where they actually act.
 * <p>
 * Selecting a user here is what the exercise means by "playing" that person: every action taken in
 * the details panel below is performed as them, and the engine checks their balance, their blocked
 * state and whether they are the market maker of the event.
 */
public class UsersTabController {

    @FXML private TableView<UserDto> usersTable;
    @FXML private TableColumn<UserDto, String> userNameColumn;
    @FXML private TableColumn<UserDto, String> userBalanceColumn;
    @FXML private TableColumn<UserDto, String> userStateColumn;

    @FXML private Label selectedUserLabel;
    @FXML private Label balanceLabel;
    @FXML private Label commissionEarnedLabel;

    @FXML private TableView<UserInvolvementDto> involvementsTable;
    @FXML private TableColumn<UserInvolvementDto, String> involvementEventColumn;
    @FXML private TableColumn<UserInvolvementDto, String> involvementRoleColumn;
    @FXML private TableColumn<UserInvolvementDto, String> involvementStatusColumn;
    @FXML private TableColumn<UserInvolvementDto, String> involvementHoldingsColumn;
    @FXML private TableColumn<UserInvolvementDto, String> involvementResultColumn;

    /** Injected by the fx:include with fx:id "userEventDetails". */
    @FXML private EventDetailsController userEventDetailsController;

    private final ObservableList<UserDto> userRows = FXCollections.observableArrayList();
    private final ObservableList<UserInvolvementDto> involvementRows = FXCollections.observableArrayList();

    private GuessMarketEngine engine;
    private Runnable onChanged = () -> { };

    @FXML
    private void initialize() {
        usersTable.setItems(userRows);
        involvementsTable.setItems(involvementRows);

        userNameColumn.setCellValueFactory(cell -> text(cell.getValue().getName()));
        userBalanceColumn.setCellValueFactory(cell -> text(money(cell.getValue().getBalance())));
        userStateColumn.setCellValueFactory(cell -> text(cell.getValue().isBlocked() ? "Blocked" : "Active"));

        involvementEventColumn.setCellValueFactory(cell -> text(cell.getValue().getEventName()));
        involvementRoleColumn.setCellValueFactory(cell -> text(describeRole(cell.getValue())));
        involvementStatusColumn.setCellValueFactory(cell -> text(describe(cell.getValue().getStatus())));
        involvementHoldingsColumn.setCellValueFactory(cell -> text(describeHoldings(cell.getValue())));
        involvementResultColumn.setCellValueFactory(
                cell -> text(cell.getValue().getStatus() == EventStatusDto.CLOSED
                        ? money(cell.getValue().getNetResult())
                        : "-"));

        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, was, is) -> onUserSelected());
        involvementsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, was, is) -> onInvolvementSelected());

        showNoUser();
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        userEventDetailsController.setEngine(engine);
        // Anything done in the details panel changes balances and event state, so the whole
        // application refreshes rather than just this tab.
        userEventDetailsController.setOnChanged(() -> {
            refresh();
            onChanged.run();
        });
    }

    /** Lets the shell refresh the events tab when something happens here. */
    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged == null ? () -> { } : onChanged;
    }

    public void refresh() {
        if (engine == null || !engine.isFileLoaded()) {
            userRows.clear();
            involvementRows.clear();
            showNoUser();
            return;
        }

        String selectedUser = selectedUserName();
        userRows.setAll(engine.getAllUsers());
        restoreUserSelection(selectedUser);
        refreshSelectedUser();
    }

    // ------------------------------------------------------------------ selection

    private void onUserSelected() {
        refreshSelectedUser();
        // A different person is now acting, so the details panel starts fresh rather than keeping
        // the previous user's selected event.
        involvementsTable.getSelectionModel().clearSelection();
        userEventDetailsController.showEvent(null, selectedUserName());
    }

    private void onInvolvementSelected() {
        UserInvolvementDto involvement = involvementsTable.getSelectionModel().getSelectedItem();
        userEventDetailsController.showEvent(
                involvement == null ? null : involvement.getEventId(),
                selectedUserName());
    }

    private void refreshSelectedUser() {
        String userName = selectedUserName();
        if (userName == null) {
            showNoUser();
            return;
        }

        UserDto user = engine.getUser(userName);
        selectedUserLabel.setText(user.getName() + (user.isBlocked() ? "  (blocked)" : ""));
        balanceLabel.setText(money(user.getBalance()));
        commissionEarnedLabel.setText(user.getTotalCommissionCollected() > 0
                ? "Commission earned as market maker: " + money(user.getTotalCommissionCollected())
                : "");

        Integer selectedEventId = selectedInvolvementEventId();
        involvementRows.setAll(engine.getUserInvolvements(userName));
        restoreInvolvementSelection(selectedEventId);
    }

    private String selectedUserName() {
        UserDto selected = usersTable.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getName();
    }

    private Integer selectedInvolvementEventId() {
        UserInvolvementDto selected = involvementsTable.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getEventId();
    }

    private void restoreUserSelection(String userName) {
        if (userName == null) {
            return;
        }
        for (UserDto user : userRows) {
            if (user.getName().equals(userName)) {
                usersTable.getSelectionModel().select(user);
                return;
            }
        }
    }

    private void restoreInvolvementSelection(Integer eventId) {
        if (eventId == null) {
            return;
        }
        for (UserInvolvementDto involvement : involvementRows) {
            if (involvement.getEventId() == eventId) {
                involvementsTable.getSelectionModel().select(involvement);
                return;
            }
        }
    }

    private void showNoUser() {
        selectedUserLabel.setText("Select a user");
        balanceLabel.setText("");
        commissionEarnedLabel.setText("");
        involvementRows.clear();
    }

    // ------------------------------------------------------------------ formatting

    private static ObservableValue<String> text(String value) {
        return new ReadOnlyStringWrapper(value);
    }

    private static String money(double amount) {
        return String.format("$%.2f", amount);
    }

    private static String describe(EventStatusDto status) {
        return switch (status) {
            case NOT_STARTED -> "Not started";
            case ACTIVE -> "Active";
            case CLOSED -> "Closed";
        };
    }

    private static String describeRole(UserInvolvementDto involvement) {
        if (involvement.isMarketMaker()) {
            return "Market maker";
        }
        return involvement.isParticipating() ? "Participant" : "Can trade";
    }

    private static String describeHoldings(UserInvolvementDto involvement) {
        List<String> parts = new ArrayList<>();
        for (HoldingDto holding : involvement.getHoldings()) {
            if (holding.getShares() > 0) {
                parts.add(String.format("%s x%d (%s)",
                        holding.getOptionName(), holding.getShares(), money(holding.getAmountPaid())));
            }
        }
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }
}
