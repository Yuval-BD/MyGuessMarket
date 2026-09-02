package gm.engine.dto;

public final class UserDto {

    private final String name;
    private final double balance;
    private final boolean blocked;
    private final double totalCommissionCollected;

    public UserDto(String name, double balance, boolean blocked, double totalCommissionCollected) {
        this.name = name;
        this.balance = balance;
        this.blocked = blocked;
        this.totalCommissionCollected = totalCommissionCollected;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public boolean isBlocked() { return blocked; }

    /** Commission earned as a market maker, across every event this user runs. */
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
}
