package gm.engine.dto;

import gm.engine.exception.InvalidEventStateException;
import gm.engine.model.ClosingOutcome;
import gm.engine.model.CommissionType;
import gm.engine.model.Event;
import gm.engine.model.EventOption;
import gm.engine.model.EventStatus;
import gm.engine.model.Holding;
import gm.engine.model.Participation;
import gm.engine.model.Trade;
import gm.engine.model.User;
import gm.engine.orderbook.Execution;
import gm.engine.orderbook.ExecutionKind;
import gm.engine.orderbook.BookStats;
import gm.engine.orderbook.Order;
import gm.engine.orderbook.OrderBook;
import gm.engine.orderbook.OrderResult;
import gm.engine.trading.LmsrTradingMethod;
import gm.engine.trading.OrderBookTradingMethod;
import gm.engine.trading.TradingMethodType;

import java.util.ArrayList;
import java.util.List;

/**
 * The single place model objects become DTOs. Nothing else converts, so the rule that no core
 * object ever escapes the engine has exactly one place to be checked.
 * <p>
 * Every DTO is built fresh on each call and holds only immutable data, so a UI holding one can
 * never observe the model changing underneath it, and can never change the model by accident.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static CommissionTypeDto toCommissionTypeDto(CommissionType type) {
        return switch (type) {
            case ON_PURCHASE -> CommissionTypeDto.ON_PURCHASE;
            case ON_CLOSE -> CommissionTypeDto.ON_CLOSE;
        };
    }

    public static EventStatusDto toEventStatusDto(EventStatus status) {
        return switch (status) {
            case NOT_STARTED -> EventStatusDto.NOT_STARTED;
            case ACTIVE -> EventStatusDto.ACTIVE;
            case CLOSED -> EventStatusDto.CLOSED;
        };
    }

    public static TradingMethodTypeDto toTradingMethodTypeDto(TradingMethodType type) {
        return switch (type) {
            case LMSR -> TradingMethodTypeDto.LMSR;
            case ORDER_BOOK -> TradingMethodTypeDto.ORDER_BOOK;
        };
    }

    public static UserDto toUserDto(User user) {
        return new UserDto(
                user.getName(),
                user.getBalance(),
                user.isBlocked(),
                user.getTotalCommissionCollected());
    }

    public static List<UserDto> toUserDtos(List<User> users) {
        List<UserDto> dtos = new ArrayList<>();
        for (User user : users) {
            dtos.add(toUserDto(user));
        }
        return dtos;
    }

    public static EventDto toEventDto(Event event) {
        List<String> optionNames = new ArrayList<>();
        for (EventOption option : event.getOptions()) {
            optionNames.add(option.getName());
        }

        return new EventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCommissionPercent(),
                toCommissionTypeDto(event.getCommissionType()),
                toTradingMethodTypeDto(event.getTradingMethod().kind()),
                toEventStatusDto(event.getStatus()),
                optionNames,
                event.getMarketMaker().getName(),
                event.getAccount().getBalance(),
                winnerNameOf(event));
    }

    public static List<EventDto> toEventDtos(List<Event> events) {
        List<EventDto> dtos = new ArrayList<>();
        for (Event event : events) {
            dtos.add(toEventDto(event));
        }
        return dtos;
    }

    /**
     * Builds the LMSR trading view. Order-book events are rejected rather than filled with
     * meaningless prices - they get their own view once the order book exists.
     */
    public static EventStateDto toEventStateDto(Event event) {
        if (!(event.getTradingMethod() instanceof LmsrTradingMethod lmsr)) {
            throw new InvalidEventStateException(String.format(
                    "Error: event \"%s\" is an order book event and has no LMSR price state.",
                    event.getName()));
        }

        long[] shares = event.getSharesArray();
        List<OptionStateDto> optionStates = new ArrayList<>();
        List<EventOption> options = event.getOptions();
        for (int i = 0; i < options.size(); i++) {
            optionStates.add(new OptionStateDto(
                    options.get(i).getName(),
                    lmsr.optionPrice(i, shares),
                    options.get(i).getSharesBought()));
        }

        return new EventStateDto(
                event.getId(),
                event.getName(),
                toEventStatusDto(event.getStatus()),
                optionStates,
                event.getAccount().getBalance(),
                event.getCommissionCollected(),
                event.getMarketMaker().getName(),
                toTradeDtosNewestFirst(event.getTrades()),
                winnerNameOf(event));
    }

    public static TradeDto toTradeDto(Trade trade) {
        return new TradeDto(
                trade.getBuyerName(),
                trade.getOption().getName(),
                trade.getQuantity(),
                trade.getSharesCost(),
                trade.getCommissionPaid(),
                trade.getTotalPaid());
    }

    /** Reversed here so no UI has to remember to do it. */
    public static List<TradeDto> toTradeDtosNewestFirst(List<Trade> trades) {
        List<TradeDto> dtos = new ArrayList<>();
        for (int i = trades.size() - 1; i >= 0; i--) {
            dtos.add(toTradeDto(trades.get(i)));
        }
        return dtos;
    }

    public static PurchaseResultDto toPurchaseResultDto(Trade trade, User buyer, Event event) {
        return new PurchaseResultDto(
                trade.getBuyerName(),
                trade.getOption().getName(),
                trade.getQuantity(),
                trade.getSharesCost(),
                trade.getCommissionPaid(),
                trade.getTotalPaid(),
                buyer.getBalance(),
                toEventStateDto(event));
    }

    public static CloseResultDto toCloseResultDto(ClosingOutcome outcome, Event event) {
        return new CloseResultDto(
                outcome.winningOptionName(),
                outcome.totalPaidToWinners(),
                outcome.totalCommissionCollected(),
                outcome.leftoverReturnedToMarketMaker(),
                lmsrStateOrNull(event));
    }

    /**
     * The LMSR price state, or null for an order-book event, which has no such thing - it has two
     * books instead. The figures that matter at close (what was paid out, the commission, the
     * leftover) are on the result itself, so an order-book close loses nothing by this being null.
     */
    private static EventStateDto lmsrStateOrNull(Event event) {
        return event.getTradingMethod() instanceof LmsrTradingMethod
                ? toEventStateDto(event)
                : null;
    }

    /**
     * Builds a user's involvement in one event. A market maker who has never traded still counts as
     * involved, so {@code participation} may legitimately be null.
     */
    public static UserInvolvementDto toInvolvementDto(Event event, Participation participation,
                                                      boolean isMarketMaker) {
        List<HoldingDto> holdings = new ArrayList<>();
        double commissionPaid = 0;
        double netResult = 0;

        if (participation != null) {
            for (Holding holding : participation.getHoldings()) {
                holdings.add(new HoldingDto(
                        holding.getOptionName(), holding.getShares(), holding.getAmountPaid()));
            }
            commissionPaid = participation.getTotalCommissionPaid();
            netResult = participation.getNetResult();
        }

        return new UserInvolvementDto(
                event.getId(),
                event.getName(),
                toTradingMethodTypeDto(event.getTradingMethod().kind()),
                toEventStatusDto(event.getStatus()),
                isMarketMaker,
                participation != null,
                holdings,
                commissionPaid,
                netResult);
    }

    /**
     * Builds the order-book view. LMSR events are rejected rather than squeezed into this shape -
     * they have a price curve and a trade history, not two books.
     */
    public static OrderBookStateDto toOrderBookStateDto(Event event) {
        if (!(event.getTradingMethod() instanceof OrderBookTradingMethod method)) {
            throw new InvalidEventStateException(String.format(
                    "Error: event \"%s\" is an LMSR event and has no order book.", event.getName()));
        }

        List<OptionBookDto> books = new ArrayList<>();
        for (EventOption option : event.getOptions()) {
            OrderBook book = event.getOrderBook(option.getName());
            books.add(new OptionBookDto(
                    option.getName(),
                    toRestingOrderDtos(book.getBids()),
                    toRestingOrderDtos(book.getAsks()),
                    toBookStatsDto(book.stats()),
                    option.getSharesBought()));
        }

        List<ParticipantDto> participants = new ArrayList<>();
        for (Participation participation : event.getParticipants()) {
            List<HoldingDto> holdings = new ArrayList<>();
            for (Holding holding : participation.getHoldings()) {
                holdings.add(new HoldingDto(
                        holding.getOptionName(), holding.getShares(), holding.getAmountPaid()));
            }
            participants.add(new ParticipantDto(
                    participation.getUserName(),
                    participation.getUser() == event.getMarketMaker(),
                    holdings,
                    participation.getTotalCommissionPaid(),
                    participation.getNetResult()));
        }

        return new OrderBookStateDto(
                event.getId(),
                event.getName(),
                toEventStatusDto(event.getStatus()),
                event.getMarketMaker().getName(),
                event.getAccount().getBalance(),
                event.getCommissionCollected(),
                method.getBaseValue(),
                method.allowsMint(),
                books,
                participants,
                winnerNameOf(event));
    }

    private static List<RestingOrderDto> toRestingOrderDtos(List<Order> orders) {
        List<RestingOrderDto> dtos = new ArrayList<>();
        for (Order order : orders) {
            dtos.add(new RestingOrderDto(
                    order.getUserName(), order.getRemainingQuantity(), order.getPricePerShare()));
        }
        return dtos;
    }

    private static BookStatsDto toBookStatsDto(BookStats stats) {
        return new BookStatsDto(stats.lastTradePrice(), stats.bestBid(), stats.bestAsk(),
                stats.mid(), stats.spread());
    }

    public static OrderResultDto toOrderResultDto(OrderResult result) {
        List<ExecutionDto> executions = new ArrayList<>();
        for (Execution execution : result.executions()) {
            executions.add(new ExecutionDto(
                    execution.kind() == ExecutionKind.MINT,
                    execution.quantity(),
                    execution.partyName(), execution.partyOptionName(), execution.partyPrice(),
                    execution.counterpartyName(), execution.counterpartyOptionName(),
                    execution.counterpartyPrice()));
        }
        return new OrderResultDto(
                result.requestedQuantity(), result.filledQuantity(), result.restingQuantity(),
                result.totalSpent(), result.totalReceived(), result.commissionPaid(), executions);
    }

    private static String winnerNameOf(Event event) {
        EventOption winner = event.getWinningOption();
        return winner == null ? null : winner.getName();
    }
}
