package gm.ui;

import gm.engine.GuessMarketEngine;
import gm.engine.exception.GuessMarketException;
import gm.engine.dto.EventDto;
import gm.engine.dto.CommissionTypeDto;
import gm.engine.dto.EventStateDto;
import gm.engine.dto.OptionStateDto;
import gm.engine.dto.TradeDto;
import gm.engine.dto.PurchaseResultDto;
import gm.engine.dto.CloseResultDto;
import java.util.List;
import java.util.Scanner;

public class GuessMarketUI {

    private final GuessMarketEngine engine;
    private final Scanner scanner;
    private boolean fileWasLoaded = false;

    public GuessMarketUI(GuessMarketEngine engine) {
        this.engine = engine;
        this.scanner = new Scanner(System.in);
    }

    public void run() {

        boolean running = true;

        while (running) {
            printMenu();
            MenuOption choice = readUserChoice();
            running = handleUserChoice(choice);
            System.out.println();
        }
    }

    private void printMenu() {

        System.out.println("What action would you like to take?:");

        for (MenuOption option : MenuOption.values()) {
            System.out.println(option.getNumber() + ". " + option.getLabel());
        }
    }

    private MenuOption readUserChoice() {

        while (true) {

            System.out.print("Enter the option number and press Enter: ");
            String line = scanner.nextLine().trim();

            try {
                int userInput = Integer.parseInt(line);
                MenuOption option = MenuOption.fromNumber(userInput);

                if (option == null) {
                    System.out.printf("Invalid option. Please enter a number between 1 and %d.%n",
                            MenuOption.values().length);
                } else {
                    return option;
                }
            } catch (NumberFormatException e) {
                System.out.println("\"" + line + "\" is not a number. Please try again.");
            }
        }
    }

    private boolean handleUserChoice(MenuOption choice) {

        if (choice == MenuOption.EXIT) {
            System.out.println("Goodbye.");
            return false;
        }

        if (choice != MenuOption.LOAD_FILE && !fileWasLoaded) {
            System.out.println("No file is loaded. Please load a valid XML file first (option "
                    + MenuOption.LOAD_FILE.getNumber() + ").");
            return true;
        }

        switch (choice) {
            case LOAD_FILE -> handleLoadFile();
            case SHOW_EVENTS -> handleShowEvents();
            case EVENT_STATE -> handleEventState();
            case PARTICIPATE -> handleParticipate();
            case CLOSE_EVENT -> handleCloseEvent();
            default -> throw new IllegalStateException("Unhandled option: " + choice);
        }
        return true;
    }

    private void handleLoadFile() {
        System.out.print("Enter the full path to the XML file: ");
        String path = scanner.nextLine().trim();

        try {
            engine.loadEventsFromFile(path);
            fileWasLoaded = true;
            int count = engine.getAllEvents().size();
            System.out.printf("The file was found valid and has been loaded successfully. %d event(s) loaded.%n", count);
        } catch (GuessMarketException e) {
            System.out.println(e.getMessage());
        }
    }

    private void handleShowEvents() {
        List<EventDto> events = engine.getAllEvents();

        if (events.isEmpty()) {
            System.out.println("There are no events loaded.");
            return;
        }

        for (EventDto event : events) {
            printEventSummary(event);
        }
    }

    private void printEventSummary(EventDto event) {
        System.out.printf("Event #%d: %s%n", event.getEventNumber(), event.getName());
        System.out.println("  Description: " + event.getDescription());
        System.out.printf("  Commission: %d%% (%s)%n",
                event.getCommissionPercent(), formatCommissionType(event.getCommissionType()));

        List<String> optionNames = event.getOptionNames();
        StringBuilder optionsLine = new StringBuilder("  Options:\n    ");
        for (int i = 0; i < optionNames.size(); i++) {
            optionsLine.append(i + 1).append(". ").append(optionNames.get(i));
            if (i < optionNames.size() - 1) {
                optionsLine.append("\n    ");
            }
        }
        System.out.println(optionsLine);
        System.out.println("  Status: " + (event.isActive() ? "ACTIVE" : "CLOSED"));
        System.out.println();
    }

    private String formatCommissionType(CommissionTypeDto type) {
        return switch (type) {
            case ON_PURCHASE -> "charged when shares are purchased";
            case ON_CLOSE -> "charged when the event closes";
        };
    }

    private void handleEventState() {
        List<EventDto> events = engine.getAllEvents();

        if (events.isEmpty()) {
            System.out.println("There are no events loaded.");
            return;
        }

        for (EventDto event : events) {
            printEventSummary(event);
        }

        int eventNumber = readIntInRange("Enter the event number to view: ", events.size());

        try {
            EventStateDto state = engine.getEventState(eventNumber);
            printEventState(state);
        } catch (GuessMarketException e) {
            System.out.println(e.getMessage());
        }
    }

    private void printEventState(EventStateDto state) {
        System.out.printf("%n=== State of Event #%d: %s ===%n", state.getEventNumber(), state.getName());

        System.out.println("Current option prices and shares bought:");
        List<OptionStateDto> options = state.getOptions();
        for (int i = 0; i < options.size(); i++) {
            OptionStateDto option = options.get(i);
            System.out.printf("  %d. %s: price %.2f, %d share(s) bought%n",
                    i + 1, option.getName(), option.getPrice(), option.getSharesBought());
        }

        System.out.printf("Event account balance: %.2f%n", state.getAccountBalance());
        System.out.printf("Total commission collected so far: %.2f%n", state.getTotalCommissionCollected());

        System.out.println("Trade history (most recent first):");
        if (state.getTrades().isEmpty()) {
            System.out.println("  No trades have been made yet.");
        } else {
            for (TradeDto trade : state.getTrades()) {
                System.out.printf("  Bought %d share(s) of \"%s\" for %.2f%n",
                        trade.getQuantity(), trade.getOptionName(), trade.getTotalPaid());
            }
        }

        if (!state.isActive()) {
            System.out.printf("%nThis event is CLOSED. Winning option: \"%s\"%n", state.getWinningOptionName());
            System.out.printf("Total paid out to winners: %.2f%n", state.getTotalPaidToWinners());
        }
        System.out.println();
    }

    private int readIntInRange(String prompt, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value < 1 || value > max) {
                    System.out.printf("Please enter a number between %d and %d.%n", 1, max);
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                System.out.println("\"" + line + "\" is not a number. Please try again.");
            }
        }
    }

    private EventDto selectEvent(List<EventDto> events, String prompt) {
        for (EventDto event : events) {
            printEventSummary(event);
        }
        int localIndex = readIntInRange(prompt, events.size());
        return events.get(localIndex - 1);
    }

    private void handleParticipate() {
        List<EventDto> activeEvents = engine.getActiveEvents();

        if (activeEvents.isEmpty()) {
            System.out.println("There are no active events to participate in.");
            return;
        }

        EventDto selectedEvent = selectEvent(activeEvents, "Enter the event number to participate in: ");
        int eventNumber = selectedEvent.getEventNumber();

        try {
            EventStateDto stateBefore = engine.getEventState(eventNumber);
            printEventState(stateBefore);

            int optionNumber = readIntInRange("Enter the option number you want to buy: ", stateBefore.getOptions().size());
            long quantity = readPositiveLong();

            PurchaseResultDto result = engine.buyShares(eventNumber, optionNumber, quantity);

            System.out.printf("%nTotal paid: %.2f (shares: %.2f, commission: %.2f)%n",
                    result.getTotalPaid(), result.getSharesCost(), result.getCommission());

            printEventState(result.getStateAfter());
        } catch (GuessMarketException e) {
            System.out.println(e.getMessage());
        }
    }

    private long readPositiveLong() {
        while (true) {
            System.out.print("Enter the quantity of shares to buy: ");
            String line = scanner.nextLine().trim();
            try {
                long value = Long.parseLong(line);
                if (value <= 0) {
                    System.out.println("Please enter a positive whole number.");
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                System.out.println("\"" + line + "\" is not a whole number. Please try again.");
            }
        }
    }

    private void handleCloseEvent() {
        List<EventDto> activeEvents = engine.getActiveEvents();

        if (activeEvents.isEmpty()) {
            System.out.println("There are no active events to close.");
            return;
        }

        EventDto selectedEvent = selectEvent(activeEvents, "Enter the event number to close: ");
        int eventNumber = selectedEvent.getEventNumber();

        try {
            EventStateDto stateBefore = engine.getEventState(eventNumber);
            printEventState(stateBefore);

            int optionNumber = readIntInRange("Enter the number of the winning option: ", stateBefore.getOptions().size());

            CloseResultDto result = engine.closeEvent(eventNumber, optionNumber);

            System.out.printf("%nEvent closed. Winning option: \"%s\"%n", result.getWinningOptionName());
            if (result.getCommissionCharged() > 0) {
                System.out.printf("Commission charged at closing: %.2f%n", result.getCommissionCharged());
            }
            System.out.printf("Total paid out to winners: %.2f%n", result.getTotalPaidToWinners());

            printEventState(result.getFinalState());
        } catch (GuessMarketException e) {
            System.out.println(e.getMessage());
        }
    }
}