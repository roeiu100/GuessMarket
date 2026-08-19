package xml;

import models.Event;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuessMarketParser {

    public static List<Event> parseEvents(String filePath) throws Exception {
        List<Event> parsedEvents = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();
        
        File xmlFile = new File(filePath.trim()); 
        if (!xmlFile.exists() || !xmlFile.getName().toLowerCase().endsWith(".xml")) {
            throw new Exception("File does not exist or is not an XML file.");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();

        NodeList eventNodes = document.getElementsByTagName("GM-event");
        
        for (int i = 0; i < eventNodes.getLength(); i++) {
            Node node = eventNodes.item(i);
            
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                Event event = new Event();
                
                event.setName(element.getAttribute("name"));
                
                int id = Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent());
                if (!seenIds.add(id)) {
                    throw new Exception("Validation Error: Duplicate Event ID found (" + id + ").");
                }
                event.setId(id);
                
                event.setDescription(element.getElementsByTagName("description").item(0).getTextContent());
                
                Element comElement = (Element) element.getElementsByTagName("comision").item(0);
                int commission = Integer.parseInt(comElement.getTextContent());
                
                if (commission < 0 || commission > 90) {
                    throw new Exception("Validation Error: Commission for event '" + event.getName() + "' must be between 0 and 90. Found: " + commission);
                }
                event.setCommission(commission);
                event.setCommissionType(comElement.getAttribute("type"));
                
                NodeList optionNodes = element.getElementsByTagName("GM-option");
                List<String> options = new ArrayList<>();
                options.add(optionNodes.item(0).getTextContent());
                options.add(optionNodes.item(1).getTextContent());
                event.setOptions(options);
                
                int b = Integer.parseInt(element.getElementsByTagName("b").item(0).getTextContent());
                event.setB(b);
                
                parsedEvents.add(event);
            }
        }
        
        return parsedEvents;
    }
}