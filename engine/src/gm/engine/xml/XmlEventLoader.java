package gm.engine.xml;

import gm.engine.exception.FileNotXmlException;
import gm.engine.exception.GmFileNotFoundException;
import gm.engine.exception.InvalidFilePathException;
import gm.engine.exception.XmlContentException;
import gm.engine.exception.XmlParseException;
import gm.engine.model.CommissionType;
import gm.engine.model.Event;
import gm.engine.model.EventOption;
import gm.engine.model.GuessMarketSystem;
import gm.engine.model.User;
import gm.engine.trading.LmsrTradingMethod;
import gm.engine.trading.OrderBookTradingMethod;
import gm.engine.trading.TradingMethod;
import gm.engine.xml.generated.model.Commission;
import gm.engine.xml.generated.model.GMEvent;
import gm.engine.xml.generated.model.GMLMSR;
import gm.engine.xml.generated.model.GMMethod;
import gm.engine.xml.generated.model.GMOrderBook;
import gm.engine.xml.generated.model.GMUser;
import gm.engine.xml.generated.model.GuessMarket;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns an exercise-2 XML file into a {@link GuessMarketSystem}.
 * <p>
 * The contract is all-or-nothing: this either returns a complete, valid system or throws. It never
 * returns something half-built, so the engine can assign the result on success and a broken file
 * can never damage the system already loaded.
 * <p>
 * Validation collects <em>every</em> problem into one {@link XmlContentException} rather than
 * stopping at the first. Someone with three mistakes in their file should see three messages, not
 * discover them one run at a time. Because of that, model constructors are never reached with bad
 * data - their own checks are invariant enforcement, not the user-facing validation path.
 */
public class XmlEventLoader {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int REQUIRED_OPTION_COUNT = 2;

    private final JAXBContext context;

    public XmlEventLoader() {
        try {
            context = JAXBContext.newInstance(GuessMarket.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialize the XML binding context.", e);
        }
    }

    public GuessMarketSystem load(String rawPath) {
        String path = validatePath(rawPath);
        File file = validateFile(path);
        GuessMarket root = unmarshal(file);

        List<GMEvent> gmEvents = (root.getGMEvents() == null)
                ? List.of()
                : root.getGMEvents().getGMEvent();
        List<GMUser> gmUsers = (root.getGMUsers() == null)
                ? List.of()
                : root.getGMUsers().getGMUser();

        List<String> problems = new ArrayList<>();

        Set<Integer> eventIds = validateEvents(gmEvents, problems);
        validateUsers(gmUsers, problems);
        Map<Integer, String> marketMakerByEventId = validateMarketMakers(gmUsers, gmEvents, eventIds, problems);

        if (!problems.isEmpty()) {
            throw new XmlContentException(problems);
        }

        return build(gmEvents, gmUsers, marketMakerByEventId);
    }

    // ------------------------------------------------------------------ file level

    private String validatePath(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            throw new InvalidFilePathException(
                    "Error: no file path was given. Please choose an XML file to load.");
        }
        String path = rawPath.trim();
        if (!path.toLowerCase().endsWith(".xml")) {
            throw new FileNotXmlException(String.format(
                    "Error: \"%s\" does not end with \".xml\". Only XML files can be loaded.", path));
        }
        return path;
    }

    private File validateFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new GmFileNotFoundException(String.format(
                    "Error: no file was found at \"%s\". Please check the path and try again.", path));
        }
        if (!file.canRead()) {
            throw new GmFileNotFoundException(String.format(
                    "Error: the file at \"%s\" could not be read. Check its permissions.", path));
        }
        return file;
    }

    /**
     * The supplied files use the {@code xsi:} prefix without ever declaring {@code xmlns:xsi}, so a
     * namespace-aware parser rejects every one of them before JAXB sees anything. Building the SAX
     * reader by hand with namespace awareness switched off is what makes them readable.
     */
    private GuessMarket unmarshal(File file) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            XMLReader reader = spf.newSAXParser().getXMLReader();
            try (FileInputStream in = new FileInputStream(file)) {
                SAXSource source = new SAXSource(reader, new InputSource(in));
                Unmarshaller unmarshaller = context.createUnmarshaller();
                Object result = unmarshaller.unmarshal(source);

                if (!(result instanceof GuessMarket guessMarket)) {
                    throw new XmlParseException(
                            "Error: the file's root element is not a valid Guess Market document.");
                }
                return guessMarket;
            }
        } catch (FileNotFoundException e) {
            throw new GmFileNotFoundException(String.format(
                    "Error: the file at \"%s\" could not be opened.", file.getPath()));
        } catch (JAXBException | SAXException | ParserConfigurationException | IOException e) {
            throw new XmlParseException(String.format(
                    "Error: the file is not valid XML. Details: %s", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------ events

    /** @return the set of event ids actually present, so the market-maker checks can use it */
    private Set<Integer> validateEvents(List<GMEvent> gmEvents, List<String> problems) {
        Set<Integer> eventIds = new LinkedHashSet<>();

        if (gmEvents.isEmpty()) {
            problems.add("The file does not define any events. At least one event is required.");
            return eventIds;
        }

        Map<Integer, String> seenIds = new LinkedHashMap<>();
        for (GMEvent gmEvent : gmEvents) {
            String label = String.format("Event #%d (\"%s\")", gmEvent.getId(), safe(gmEvent.getName()));

            if (isBlank(gmEvent.getName())) {
                problems.add(label + ": name must not be blank.");
            }
            if (isBlank(gmEvent.getDescription())) {
                problems.add(label + ": description must not be blank.");
            }

            int id = gmEvent.getId();
            if (seenIds.containsKey(id)) {
                problems.add(String.format(
                        "%s: id %d is already used by %s. Each event must have a unique id.",
                        label, id, seenIds.get(id)));
            } else {
                seenIds.put(id, label);
                eventIds.add(id);
            }

            validateCommission(gmEvent.getCommission(), label, problems);
            validateOptions(gmEvent, label, problems);
            validateMethod(gmEvent.getGMMethod(), label, problems);
        }

        return eventIds;
    }

    private void validateCommission(Commission commission, String label, List<String> problems) {
        if (commission == null) {
            problems.add(label + ": commission details are missing.");
            return;
        }
        int percent = commission.getValue();
        if (percent < MIN_COMMISSION || percent > MAX_COMMISSION) {
            problems.add(String.format("%s: commission is %d%%. It must be between %d and %d.",
                    label, percent, MIN_COMMISSION, MAX_COMMISSION));
        }
        String type = commission.getType();
        if (!"on-close".equalsIgnoreCase(type) && !"on-purchase".equalsIgnoreCase(type)) {
            problems.add(String.format(
                    "%s: unknown commission type \"%s\". Expected \"on-purchase\" or \"on-close\".",
                    label, safe(type)));
        }
    }

    private void validateOptions(GMEvent gmEvent, String label, List<String> problems) {
        List<String> optionNames = (gmEvent.getGMOptions() == null)
                ? List.of()
                : gmEvent.getGMOptions().getGMOption();

        if (optionNames.size() != REQUIRED_OPTION_COUNT) {
            problems.add(String.format("%s: must have exactly %d options, but found %d.",
                    label, REQUIRED_OPTION_COUNT, optionNames.size()));
            return;
        }

        String first = safe(optionNames.getFirst());
        String second = safe(optionNames.get(1));
        if (first.isEmpty() || second.isEmpty()) {
            problems.add(label + ": option names must not be blank.");
        } else if (first.equalsIgnoreCase(second)) {
            problems.add(String.format(
                    "%s: both options are named \"%s\". Options must have different names.",
                    label, first));
        }
    }

    private void validateMethod(GMMethod method, String label, List<String> problems) {
        if (method == null) {
            problems.add(label + ": trading method details are missing.");
            return;
        }

        GMLMSR lmsr = method.getGMLMSR();
        GMOrderBook orderBook = method.getGMOrderBook();

        if (lmsr == null && orderBook == null) {
            problems.add(label + ": must define either an LMSR method or an order book method.");
            return;
        }
        if (lmsr != null && orderBook != null) {
            problems.add(label + ": defines both LMSR and order book. An event has exactly one method.");
            return;
        }

        if (lmsr != null) {
            if (lmsr.getB() <= 0) {
                problems.add(String.format(
                        "%s: liquidity parameter b must be a positive integer, but found %d.",
                        label, lmsr.getB()));
            }
            return;
        }

        int baseValue = orderBook.getD();
        int initial = orderBook.getInitial();

        if (baseValue <= 0) {
            problems.add(String.format(
                    "%s: base value d must be a positive integer, but found %d.", label, baseValue));
        }
        if (initial < 0) {
            problems.add(String.format(
                    "%s: initial investment cannot be negative, but found %d.", label, initial));
        }
        // A part-pair would strand money in the event account that nobody is entitled to.
        if (baseValue > 0 && initial > 0 && initial % baseValue != 0) {
            problems.add(String.format(
                    "%s: initial investment %d is not a whole multiple of the base value %d, "
                            + "so it does not buy a whole number of share pairs.",
                    label, initial, baseValue));
        }
        if (!isBoolean(orderBook.getAllowMint())) {
            problems.add(String.format(
                    "%s: allow-mint must be \"true\" or \"false\", but found \"%s\".",
                    label, safe(orderBook.getAllowMint())));
        }
    }

    // ------------------------------------------------------------------ users

    private void validateUsers(List<GMUser> gmUsers, List<String> problems) {
        if (gmUsers.isEmpty()) {
            problems.add("The file does not define any users. At least one user is required.");
            return;
        }

        Set<String> seenNames = new LinkedHashSet<>();
        for (GMUser gmUser : gmUsers) {
            String name = safe(gmUser.getName());
            String label = String.format("User \"%s\"", name);

            if (name.isEmpty()) {
                problems.add("A user has a blank name. Every user must have a name.");
            } else if (!seenNames.add(name)) {
                problems.add(String.format(
                        "%s: this name is used more than once. Every user must have a unique name.", label));
            }

            // The schema permits 0, but a user with no money can never do anything.
            if (gmUser.getInitialCash() <= 0) {
                problems.add(String.format(
                        "%s: initial cash must be greater than 0, but found %d.",
                        label, gmUser.getInitialCash()));
            }
        }
    }

    /**
     * Cross-checks the market-maker declarations against the events.
     *
     * @return event id to the name of its market maker, used when building the events
     */
    private Map<Integer, String> validateMarketMakers(List<GMUser> gmUsers, List<GMEvent> gmEvents,
                                                      Set<Integer> eventIds, List<String> problems) {

        Map<Integer, List<String>> claimsByEventId = new LinkedHashMap<>();

        for (GMUser gmUser : gmUsers) {
            String userName = safe(gmUser.getName());
            if (gmUser.getGMMarketMaker() == null) {
                continue;                      // an ordinary user - the element is optional
            }
            for (gm.engine.xml.generated.model.Event reference : gmUser.getGMMarketMaker().getEvent()) {
                int referencedId = reference.getId();
                if (!eventIds.contains(referencedId)) {
                    problems.add(String.format(
                            "User \"%s\": declared market maker of event id %d, which does not exist in this file.",
                            userName, referencedId));
                    continue;
                }
                claimsByEventId.computeIfAbsent(referencedId, _ -> new ArrayList<>()).add(userName);
            }
        }

        Map<Integer, String> marketMakerByEventId = new LinkedHashMap<>();
        for (GMEvent gmEvent : gmEvents) {
            int id = gmEvent.getId();
            String label = String.format("Event #%d (\"%s\")", id, safe(gmEvent.getName()));
            List<String> claims = claimsByEventId.get(id);

            if (claims == null || claims.isEmpty()) {
                problems.add(label + ": no user is declared as its market maker. Every event needs exactly one.");
            } else if (claims.size() > 1) {
                long distinct = claims.stream().distinct().count();
                if (distinct == 1) {
                    problems.add(String.format(
                            "%s: user \"%s\" declares this event %d times. Declare it once.",
                            label, claims.getFirst(), claims.size()));
                } else {
                    problems.add(String.format(
                            "%s: declared as market maker by %s. Every event needs exactly one.",
                            label, String.join(", ", claims)));
                }
            } else {
                marketMakerByEventId.put(id, claims.getFirst());
            }
        }

        return marketMakerByEventId;
    }

    // ------------------------------------------------------------------ building

    private GuessMarketSystem build(List<GMEvent> gmEvents, List<GMUser> gmUsers,
                                    Map<Integer, String> marketMakerByEventId) {

        List<User> users = new ArrayList<>();
        Map<String, User> usersByName = new LinkedHashMap<>();
        for (GMUser gmUser : gmUsers) {
            User user = new User(gmUser.getName(), gmUser.getInitialCash());
            users.add(user);
            usersByName.put(user.getName(), user);
        }

        List<Event> events = new ArrayList<>();
        for (GMEvent gmEvent : gmEvents) {
            User marketMaker = usersByName.get(marketMakerByEventId.get(gmEvent.getId()));
            events.add(mapEvent(gmEvent, marketMaker));
        }

        return new GuessMarketSystem(events, users);
    }

    private Event mapEvent(GMEvent gmEvent, User marketMaker) {
        Commission commission = gmEvent.getCommission();

        List<EventOption> options = new ArrayList<>();
        for (String rawName : gmEvent.getGMOptions().getGMOption()) {
            options.add(new EventOption(rawName));
        }

        return new Event(
                gmEvent.getId(),
                safe(gmEvent.getName()),
                safe(gmEvent.getDescription()),
                commission.getValue(),
                CommissionType.fromXmlValue(commission.getType()),
                options,
                mapTradingMethod(gmEvent.getGMMethod()),
                marketMaker);
    }

    private TradingMethod mapTradingMethod(GMMethod method) {
        if (method.getGMLMSR() != null) {
            return new LmsrTradingMethod(method.getGMLMSR().getB());
        }
        GMOrderBook orderBook = method.getGMOrderBook();
        return new OrderBookTradingMethod(
                orderBook.getInitial(),
                orderBook.getD(),
                Boolean.parseBoolean(safe(orderBook.getAllowMint())));
    }

    // ------------------------------------------------------------------ helpers

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBoolean(String value) {
        String trimmed = safe(value);
        return "true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed);
    }
}
