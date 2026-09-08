package gm.engine.dto;

public final class OptionStateDto {

    private final String name;
    private final double price;
    private final long sharesOutstanding;

    public OptionStateDto(String name, double price, long sharesOutstanding) {
        this.name = name;
        this.price = price;
        this.sharesOutstanding = sharesOutstanding;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public long getSharesOutstanding() { return sharesOutstanding; }
}