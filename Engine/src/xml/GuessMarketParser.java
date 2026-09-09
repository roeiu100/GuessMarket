package xml;

import models.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Parses Guess Market XML configuration files.
 * Supports both EX1 format (events only) and EX2 format (events + users + Order Book).
 */
public class GuessMarketParser {

    /**
     * Parses an EX1-format XML file (events only, no users).
     * Kept for backward compatibility.
     */
    public static List<Event> parseEvents(String filePath) throws Exception {
        ParseResult result = parseFile(filePath);
        return result.getEvents();
    }

    /**
     * Parses an EX2-format XML file containing events and users.
     * Performs full validation including:
     * - File existence and XML extension
     * - Unique event IDs
     * - Commission range (0-90)
     * - Unique user names
     * - Initial cash > 0
     * - Valid MM event references
     * - Every event has exactly one MM
     *
     * @param filePath the path to the XML file
     * @return ParseResult containing events and users
     * @throws Exception if the file is invalid
     */
    public static ParseResult parseFile(String filePath) throws Exception {
        File xmlFile = new File(filePath.trim());
        if (!xmlFile.exists()) {
            throw new Exception("File does not exist: " + filePath);
        }
        if (!xmlFile.getName().toLowerCase().endsWith(".xml")) {
            throw new Exception("File is not an XML file: " + xmlFile.getName());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        // Parse events
        List<Event> events = parseEventNodes(document);

        // Parse users (may be absent in EX1 format)
        List<User> users = parseUserNodes(document, events);

        // Validate MM assignments if users are present
        if (!users.isEmpty()) {
            validateMmAssignments(events, users);
        }

        return new ParseResult(events, users);
    }

    /**
     * Parses all GM-event elements from the document.
     */
    private static List<Event> parseEventNodes(Document document) throws Exception {
        List<Event> parsedEvents = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        NodeList eventNodes = document.getElementsByTagName("GM-event");

        for (int i = 0; i < eventNodes.getLength(); i++) {
            Node node = eventNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element element = (Element) node;
            Event event = new Event();

            // Name (attribute)
            event.setName(element.getAttribute("name"));

            // ID
            int id = Integer.parseInt(getElementText(element, "id"));
            if (!seenIds.add(id)) {
                throw new Exception("Validation Error: Duplicate Event ID found (" + id + ").");
            }
            event.setId(id);

            // Description
            event.setDescription(getElementText(element, "description"));

            // Commission - support both spellings: "commission" (EX2) and "comision" (EX1)
            Element comElement = getFirstElement(element, "commission");
            if (comElement == null) {
                comElement = getFirstElement(element, "comision");
            }
            if (comElement == null) {
                throw new Exception("Validation Error: Missing commission element for event '" + event.getName() + "'.");
            }

            int commission = Integer.parseInt(comElement.getTextContent().trim());
            if (commission < 0 || commission > 90) {
                throw new Exception("Validation Error: Commission for event '" + event.getName() +
                        "' must be between 0 and 90. Found: " + commission);
            }
            event.setCommission(commission);
            event.setCommissionType(comElement.getAttribute("type"));

            // Options
            NodeList optionNodes = element.getElementsByTagName("GM-option");
            List<String> options = new ArrayList<>();
            for (int j = 0; j < optionNodes.getLength(); j++) {
                options.add(optionNodes.item(j).getTextContent().trim());
            }
            event.setOptions(options);

            // Trading method: GM-LMSR or GM-order-book
            Element methodElement = getFirstElement(element, "GM-method");
            if (methodElement != null) {
                Element lmsrElement = getFirstElement(methodElement, "GM-LMSR");
                Element obElement = getFirstElement(methodElement, "GM-order-book");

                if (lmsrElement != null) {
                    event.setTradingMethod(TradingMethod.LMSR);
                    int b = Integer.parseInt(getElementText(lmsrElement, "b"));
                    event.setB(b);
                } else if (obElement != null) {
                    event.setTradingMethod(TradingMethod.ORDER_BOOK);
                    int initial = Integer.parseInt(obElement.getAttribute("initial"));
                    int d = Integer.parseInt(obElement.getAttribute("d"));
                    boolean allowMint = Boolean.parseBoolean(obElement.getAttribute("allow-mint"));
                    event.setObConfig(new OrderBookConfig(initial, d, allowMint));
                } else {
                    throw new Exception("Validation Error: Event '" + event.getName() +
                            "' has no recognized trading method (expected GM-LMSR or GM-order-book).");
                }
            }

            parsedEvents.add(event);
        }

        return parsedEvents;
    }

    /**
     * Parses all GM-user elements from the document.
     * Returns an empty list if no GM-users section is found (EX1 format).
     */
    private static List<User> parseUserNodes(Document document, List<Event> events) throws Exception {
        List<User> users = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        Map<Integer, Event> eventMap = new HashMap<>();
        for (Event e : events) {
            eventMap.put(e.getId(), e);
        }

        NodeList userNodes = document.getElementsByTagName("GM-user");
        if (userNodes.getLength() == 0) {
            return users; // EX1 format: no users section
        }

        for (int i = 0; i < userNodes.getLength(); i++) {
            Node node = userNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element element = (Element) node;

            // Name (attribute) - must be unique
            String name = element.getAttribute("name").trim();
            if (!seenNames.add(name.toLowerCase())) {
                throw new Exception("Validation Error: Duplicate user name found: '" + name + "'.");
            }

            // Initial cash
            int initialCash = Integer.parseInt(getElementText(element, "initial-cash"));
            if (initialCash <= 0) {
                throw new Exception("Validation Error: Initial cash for user '" + name +
                        "' must be greater than 0. Found: " + initialCash);
            }

            User user = new User(name, initialCash);

            // Market Maker assignments (optional)
            Element mmElement = getFirstElement(element, "GM-market-maker");
            if (mmElement != null) {
                NodeList eventRefs = mmElement.getElementsByTagName("event");
                for (int j = 0; j < eventRefs.getLength(); j++) {
                    Element eventRef = (Element) eventRefs.item(j);
                    int eventId = Integer.parseInt(eventRef.getAttribute("id"));

                    // Validate that the referenced event exists
                    if (!eventMap.containsKey(eventId)) {
                        throw new Exception("Validation Error: User '" + name +
                                "' references non-existent event ID: " + eventId);
                    }

                    user.getMmEventIds().add(eventId);

                    // Set the MM on the event
                    Event referencedEvent = eventMap.get(eventId);
                    if (referencedEvent.getMmUserName() != null) {
                        throw new Exception("Validation Error: Event '" + referencedEvent.getName() +
                                "' (ID: " + eventId + ") already has a Market Maker assigned: '"
                                + referencedEvent.getMmUserName() + "'. Cannot assign '" + name + "'.");
                    }
                    referencedEvent.setMmUserName(name);
                }
            }

            users.add(user);
        }

        return users;
    }

    /**
     * Validates that every event has exactly one MM assigned.
     */
    private static void validateMmAssignments(List<Event> events, List<User> users) throws Exception {
        for (Event event : events) {
            if (event.getMmUserName() == null || event.getMmUserName().isEmpty()) {
                throw new Exception("Validation Error: Event '" + event.getName() +
                        "' (ID: " + event.getId() + ") has no Market Maker assigned.");
            }
        }
    }

    // --- Helper methods ---

    /**
     * Gets the text content of the first child element with the given tag name.
     */
    private static String getElementText(Element parent, String tagName) throws Exception {
        Element child = getFirstElement(parent, tagName);
        if (child == null) {
            throw new Exception("Missing required element: " + tagName);
        }
        return child.getTextContent().trim();
    }

    /**
     * Gets the first child element with the given tag name, or null if not found.
     */
    private static Element getFirstElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        Node node = nodes.item(0);
        return (node.getNodeType() == Node.ELEMENT_NODE) ? (Element) node : null;
    }
}