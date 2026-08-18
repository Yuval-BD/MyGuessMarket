package gm.engine;

import gm.engine.dto.*;
import gm.engine.exception.EventNotActiveException;
import gm.engine.exception.NoFileLoadedException;
import gm.engine.model.CommissionType;
import gm.engine.model.Event;
import gm.engine.model.EventOption;
import gm.engine.model.GuessMarketSystem;
import gm.engine.model.Trade;
import gm.engine.xml.XmlEventLoader;

import java.util.List;

public class GuessMarketEngineImpl implements GuessMarketEngine {

    private final XmlEventLoader loader = new XmlEventLoader();
    private GuessMarketSystem system;

    @Override
    public void loadEventsFromFile(String fullPath) {
        this.system = loader.load(fullPath);
    }

    @Override
    public List<EventDto> getAllEvents() {
        requireFileLoaded();
        return system.getEvents().stream().map(DtoMapper::toEventDto).toList();
    }

    @Override
    public List<EventDto> getActiveEvents() {
        requireFileLoaded();
        return system.getEvents().stream()
                .filter(Event::isActive)
                .map(DtoMapper::toEventDto)
                .toList();
    }

    @Override
    public EventStateDto getEventState(int eventNumber) {
        requireFileLoaded();
        Event event = system.getEvent(eventNumber);
        return DtoMapper.toEventStateDto(event);
    }

    @Override
    public PurchaseResultDto buyShares(int eventNumber, int optionNumber, long quantity) {
        requireFileLoaded();
        Event event = system.getEvent(eventNumber);

        if (!event.isActive()) {
            throw new EventNotActiveException(String.format(
                    "Error: event \"%s\" is closed. You cannot buy shares in a closed event.", event.getName()));
        }

        EventOption option = event.getOption(optionNumber);
        long[] sharesBefore = event.getSharesArray();
        int index = optionNumber - 1;

        double sharesCost = event.getMarketMaker().costOfBuying(index, quantity, sharesBefore);

        double commission = (event.getCommissionType() == CommissionType.ON_PURCHASE)
                ? sharesCost * event.getCommissionPercent() / 100.0
                : 0.0;
        double totalPaid = sharesCost + commission;

        option.addShares(quantity);
        event.getAccount().deposit(sharesCost);
        event.getAccount().depositCommission(commission);   // 0 when ON_CLOSE — harmless

        event.recordTrade(new Trade(option, quantity, sharesCost, commission));

        EventStateDto stateAfter = DtoMapper.toEventStateDto(event);
        return new PurchaseResultDto(sharesCost, commission, totalPaid, stateAfter);
    }

    @Override
    public CloseResultDto closeEvent(int eventNumber, int optionNumber) {
        requireFileLoaded();
        Event event = system.getEvent(eventNumber);
        EventOption winner = event.getOption(optionNumber);

        event.close(winner);   // throws if already closed, or if winner isn't one of this event's options

        long winningShares = winner.getSharesBought();
        double gross = winningShares * 1.0;
        double commissionCharged = 0.0;

        if (event.getCommissionType() == CommissionType.ON_CLOSE) {
            commissionCharged = gross * event.getCommissionPercent() / 100.0;
            event.getAccount().recordCommission(commissionCharged);
        }

        double payout = gross - commissionCharged;
        event.getAccount().withdraw(payout);

        EventStateDto finalState = DtoMapper.toEventStateDto(event);
        return new CloseResultDto(winner.getName(), commissionCharged, payout, finalState);
    }

    private void requireFileLoaded() {
        if (system == null) {
            throw new NoFileLoadedException("Error: no XML file has been loaded yet. Please load a file first.");
        }
    }
}