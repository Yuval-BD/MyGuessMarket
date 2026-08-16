package gm.engine.market;

public interface MarketMaker {

    double optionPrice(int optionIndex, long[] shares);

    double costOfBuying(int optionIndex, long quantity, long[] shares);

    double initialSubsidy(int numberOfOptions);
}