package gm.ui;

import gm.engine.GuessMarketEngine;

public class Main {
    public static void main(String[] args) {
        new GuessMarketUI(new GuessMarketEngine()).run();
    }
}
