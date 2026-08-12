package gm.ui;

public enum MenuOption {
    LOAD_FILE("Read event details from XML file"),
    SHOW_EVENTS("Show all events"),
    EVENT_STATE("Show trading state of an event"),
    PARTICIPATE("Participate in an event"),
    CLOSE_EVENT("Close an event"),
    EXIT("Exit");

    private final String label;

    MenuOption(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public int getNumber() {
        return ordinal() + 1;
    }

    public static MenuOption fromNumber(int number) {
        MenuOption[] all = values();
        if (number < 1 || number > all.length) {
            return null;
        }
        return all[number - 1];
    }
}