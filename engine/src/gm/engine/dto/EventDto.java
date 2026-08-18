package gm.engine.dto;

import java.util.List;

public final class EventDto {

    private final int eventNumber;
    private final int id;
    private final String name;
    private final String description;
    private final int commissionPercent;
    private final CommissionTypeDto commissionType;
    private final List<String> optionNames;
    private final boolean active;

    public EventDto(int eventNumber, int id, String name, String description,
                    int commissionPercent, CommissionTypeDto commissionType,
                    List<String> optionNames, boolean active) {
        this.eventNumber = eventNumber;
        this.id = id;
        this.name = name;
        this.description = description;
        this.commissionPercent = commissionPercent;
        this.commissionType = commissionType;
        this.optionNames = List.copyOf(optionNames);
        this.active = active;
    }

    public int getEventNumber() { return eventNumber; }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getCommissionPercent() { return commissionPercent; }
    public CommissionTypeDto getCommissionType() { return commissionType; }
    public List<String> getOptionNames() { return optionNames; }
    public boolean isActive() { return active; }
}