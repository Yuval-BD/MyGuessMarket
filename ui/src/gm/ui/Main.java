package gm.ui;

import gm.engine.GuessMarketEngine;
import gm.engine.GuessMarketEngineImpl;

public class Main {
    public static void main(String[] args) {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        new GuessMarketUI(engine).run();
    }
}