package gm.engine.market;

import gm.engine.exception.InvalidEventDataException;
import gm.engine.exception.InvalidQuantityException;

public class LmsrMarketMaker implements MarketMaker {

    private final int liquidityParameter;

    public LmsrMarketMaker(int liquidityParameter) {
        if (liquidityParameter <= 0) {
            throw new InvalidEventDataException(String.format(
                    "Error: liquidity parameter must be a positive integer, but got %d.", liquidityParameter));
        }
        this.liquidityParameter = liquidityParameter;
    }

    @Override
    public double optionPrice(int optionIndex, long[] shares) {
        double[] x = toExponents(shares);
        double m = max(x);
        double sum = 0;
        for (double xi : x) {
            sum += Math.exp(xi - m);
        }
        return Math.exp(x[optionIndex] - m) / sum;
    }

    @Override
    public double costOfBuying(int optionIndex, long quantity, long[] shares) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(
                    String.format("Error: quantity must be positive, but got %d.", quantity));
        }
        double before = cost(shares);
        long[] after = shares.clone();
        after[optionIndex] += quantity;
        return cost(after) - before;
    }

    @Override
    public double initialSubsidy(int numberOfOptions) {
        return cost(new long[numberOfOptions]);
    }

    private double cost(long[] shares) {
        double[] x = toExponents(shares);
        double m = max(x);
        double sum = 0;
        for (double xi : x) {
            sum += Math.exp(xi - m);
        }
        return liquidityParameter * (m + Math.log(sum));
    }

    private double[] toExponents(long[] shares) {
        double[] x = new double[shares.length];
        for (int i = 0; i < shares.length; i++) {
            x[i] = (double) shares[i] / liquidityParameter;
        }
        return x;
    }

    private double max(double[] values) {
        double m = values[0];
        for (double v : values) {
            if (v > m) m = v;
        }
        return m;
    }
}