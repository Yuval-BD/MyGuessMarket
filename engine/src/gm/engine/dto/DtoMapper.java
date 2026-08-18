package gm.engine.dto;

import gm.engine.model.Event;
import gm.engine.model.EventOption;
import gm.engine.model.CommissionType;
import gm.engine.model.Trade;

import java.util.ArrayList;
import java.util.List;

public final class DtoMapper {

    private DtoMapper() { }

    public static CommissionTypeDto toCommissionTypeDto(CommissionType type) {
        return switch (type) {
            case ON_PURCHASE -> CommissionTypeDto.ON_PURCHASE;
            case ON_CLOSE -> CommissionTypeDto.ON_CLOSE;
        };
    }

    public static EventDto toEventDto(Event event) {
        List<String> optionNames = event.getOptions().stream()
                .map(EventOption::getName)
                .toList();

        return new EventDto(
                event.getEventNumber(),
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPercent(),
                toCommissionTypeDto(event.getCommissionType()),
                optionNames,
                event.isActive()
        );
    }

    public static EventStateDto toEventStateDto(Event event) {
        long[] shares = event.getSharesArray();
        List<EventOption> eventOptions = event.getOptions();

        List<OptionStateDto> options = new ArrayList<>();
        for (int i = 0; i < eventOptions.size(); i++) {
            double price = event.getMarketMaker().optionPrice(i, shares);
            options.add(new OptionStateDto(eventOptions.get(i).getName(), price, eventOptions.get(i).getSharesBought()));
        }

        List<TradeDto> trades = new ArrayList<>();
        List<Trade> eventTrades = event.getTrades();
        for (int i = eventTrades.size() - 1; i >= 0; i--) {   // newest first
            Trade t = eventTrades.get(i);
            trades.add(new TradeDto(t.getOption().getName(), t.getQuantity(),
                    t.getSharesCost(), t.getCommissionPaid(), t.getTotalPaid()));
        }

        String winningOptionName = null;
        Double totalPaidToWinners = null;
        if (!event.isActive()) {
            winningOptionName = event.getWinningOption().getName();
            long winningShares = event.getWinningOption().getSharesBought();
            double gross = winningShares * 1.0;
            double commissionCharged = (event.getCommissionType() == CommissionType.ON_CLOSE)
                    ? gross * event.getCommissionPercent() / 100.0
                    : 0.0;
            totalPaidToWinners = gross - commissionCharged;
        }

        return new EventStateDto(
                event.getEventNumber(),
                event.getId(),
                event.getName(),
                event.isActive(),
                options,
                event.getAccount().getBalance(),
                event.getAccount().getTotalCommissionCollected(),
                trades,
                winningOptionName,
                totalPaidToWinners
        );
    }
}