package gm.ui;

import gm.engine.GuessMarketEngine;
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
            case LOAD_FILE -> System.out.println("Not implemented yet.");
            case SHOW_EVENTS -> System.out.println("Not implemented yet.");
            case EVENT_STATE -> System.out.println("Not implemented yet.");
            case PARTICIPATE -> System.out.println("Not implemented yet.");
            case CLOSE_EVENT -> System.out.println("Not implemented yet.");
            default -> throw new IllegalStateException("Unhandled option: " + choice);
        }
        return true;
    }
}