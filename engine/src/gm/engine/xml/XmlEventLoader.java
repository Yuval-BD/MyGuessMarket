package gm.engine.xml;

import gm.engine.exception.*;
import gm.engine.market.LmsrMarketMaker;
import gm.engine.market.MarketMaker;
import gm.engine.model.CommissionType;
import gm.engine.model.Event;
import gm.engine.model.EventOption;
import gm.engine.model.GuessMarketSystem;
import gm.engine.xml.generated.model.Comision;
import gm.engine.xml.generated.model.GMEvent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlEventLoader {

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

        List<String> problems = new ArrayList<>();
        Map<Integer, String> seenIds = new HashMap<>();

        for (int i = 0; i < gmEvents.size(); i++) {
            validateEvent(gmEvents.get(i), i + 1, seenIds, problems);
        }
        if (gmEvents.isEmpty()) {
            problems.add("The file does not define any events. At least one event is required.");
        }
        if (!problems.isEmpty()) {
            throw new XmlContentException(problems);
        }

        List<Event> events = new ArrayList<>();
        for (int i = 0; i < gmEvents.size(); i++) {
            events.add(mapEvent(gmEvents.get(i), i + 1));
        }
        return new GuessMarketSystem(events);
    }

    private String validatePath(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            throw new InvalidFilePathException(
                    "Error: no file path was given. Please enter a full path to an XML file.");
        }
        String path = rawPath.trim();
        if (!path.toLowerCase().endsWith(".xml")) {
            throw new FileNotXmlException(
                    String.format("Error: \"%s\" does not end with \".xml\". Only XML files can be loaded.", path));
        }
        return path;
    }

    private File validateFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new GmFileNotFoundException(
                    String.format("Error: no file was found at \"%s\". Please check the path and try again.", path));
        }
        if (!file.canRead()) {
            throw new GmFileNotFoundException(
                    String.format("Error: the file at \"%s\" could not be read. Check its permissions.", path));
        }
        return file;
    }

    private GuessMarket unmarshal(File file) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false);
            // Cheap defense against two well-known XML attack patterns
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            XMLReader reader = spf.newSAXParser().getXMLReader();
            try (FileInputStream in = new FileInputStream(file)) {
                SAXSource source = new SAXSource(reader, new InputSource(in));

                Unmarshaller unmarshaller = context.createUnmarshaller();   // <- uses the field now
                Object result = unmarshaller.unmarshal(source);

                if (!(result instanceof GuessMarket guessMarket)) {
                    throw new XmlParseException("Error: the file's root element is not a valid Guess Market document.");
                }
                return guessMarket;
            }
        } catch (FileNotFoundException e) {
            throw new GmFileNotFoundException(
                    String.format("Error: the file at \"%s\" could not be opened.", file.getPath()));
        } catch (JAXBException | SAXException | ParserConfigurationException | IOException e) {
            throw new XmlParseException(
                    String.format("Error: the file is not valid XML. Details: %s", e.getMessage()));
        }
    }

    private void validateEvent(GMEvent gmEvent, int eventNumber, Map<Integer, String> seenIds, List<String> problems) {
        String label = String.format("Event #%d (\"%s\")", eventNumber, safe(gmEvent.getName()));

        if (isBlank(gmEvent.getName())) {
            problems.add(label + ": name must not be blank.");
        }
        if (isBlank(gmEvent.getDescription())) {
            problems.add(label + ": description must not be blank.");
        }

        int id = gmEvent.getId();
        if (seenIds.containsKey(id)) {
            problems.add(String.format("%s: id %d is already used by %s. Each event must have a unique id.",
                    label, id, seenIds.get(id)));
        } else {
            seenIds.put(id, label);
        }

        Comision comision = gmEvent.getComision();
        if (comision == null) {
            problems.add(label + ": commission details are missing.");
        } else {
            int commission = comision.getValue();
            if (commission < 0 || commission > 90) {
                problems.add(String.format("%s: commission is %d%%. It must be between 0 and 90.", label, commission));
            }
            if (!"on-close".equalsIgnoreCase(comision.getType()) && !"on-purchase".equalsIgnoreCase(comision.getType())) {
                problems.add(String.format("%s: unknown commission type \"%s\". Expected \"on-purchase\" or \"on-close\".",
                        label, comision.getType()));
            }
        }

        List<String> optionNames = (gmEvent.getGMOptions() == null) ? List.of() : gmEvent.getGMOptions().getGMOption();
        if (optionNames.size() != 2) {
            problems.add(String.format("%s: must have exactly 2 options, but found %d.", label, optionNames.size()));
        } else {
            String opt1 = optionNames.get(0) == null ? "" : optionNames.get(0).trim();
            String opt2 = optionNames.get(1) == null ? "" : optionNames.get(1).trim();
            if (opt1.isEmpty() || opt2.isEmpty()) {
                problems.add(label + ": option names must not be blank.");
            } else if (opt1.equalsIgnoreCase(opt2)) {
                problems.add(String.format("%s: both options are named \"%s\". Options must have different names.",
                        label, opt1));
            }
        }

        if (gmEvent.getGMMethod() == null || gmEvent.getGMMethod().getGMLMSR() == null) {
            problems.add(label + ": LMSR method details are missing.");
        } else {
            int b = gmEvent.getGMMethod().getGMLMSR().getB();
            if (b <= 0) {
                problems.add(String.format("%s: liquidity parameter b must be a positive integer, but found %d.", label, b));
            }
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private Event mapEvent(GMEvent gmEvent, int eventNumber) {
        Comision comision = gmEvent.getComision();
        CommissionType commissionType = CommissionType.fromXmlValue(comision.getType());

        List<EventOption> options = new ArrayList<>();
        for (String rawName : gmEvent.getGMOptions().getGMOption()) {
            options.add(new EventOption(rawName));
        }

        int liquidityParameter = gmEvent.getGMMethod().getGMLMSR().getB();
        MarketMaker marketMaker = new LmsrMarketMaker(liquidityParameter);

        return new Event(
                eventNumber,
                gmEvent.getId(),
                gmEvent.getName().trim(),
                gmEvent.getDescription().trim(),
                comision.getValue(),
                commissionType,
                options,
                marketMaker
        );
    }
}