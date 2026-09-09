package xml;

import models.Event;
import models.User;
import java.util.List;

/**
 * Wrapper for the result of parsing an EX2 XML file.
 * Contains both the parsed events and users.
 */
public class ParseResult {
    private List<Event> events;
    private List<User> users;

    public ParseResult(List<Event> events, List<User> users) {
        this.events = events;
        this.users = users;
    }

    public List<Event> getEvents() { return events; }
    public List<User> getUsers() { return users; }
}
