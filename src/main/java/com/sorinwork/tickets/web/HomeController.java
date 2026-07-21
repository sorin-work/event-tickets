package com.sorinwork.tickets.web;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.TierAvailability;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

  private final EventService events;
  private final AvailabilityService availability;

  public HomeController(EventService events, AvailabilityService availability) {
    this.events = events;
    this.availability = availability;
  }

  @GetMapping("/")
  public String home(@RequestParam(defaultValue = "") String q, Model model) {
    List<Event> upcoming = events.upcomingPublished();
    model.addAttribute("q", q);
    model.addAttribute("hasEvents", !upcoming.isEmpty());
    List<Event> shown = matching(upcoming, q);
    Map<Long, Integer> left = new HashMap<>();
    for (Event event : shown) {
      left.put(
          event.getId(),
          availability.forEvent(event.getId()).values().stream()
              .mapToInt(TierAvailability::remaining)
              .sum());
    }
    model.addAttribute("events", shown);
    model.addAttribute("left", left);
    return "public/home";
  }

  @GetMapping("/events/past")
  public String past(Model model) {
    model.addAttribute("events", events.recentPast());
    return "public/past";
  }

  /** Events whose title or venue contains every word of the query, ignoring case and accents. */
  static List<Event> matching(List<Event> events, String query) {
    String[] words = fold(query).split("\\s+");
    return events.stream()
        .filter(
            event -> {
              String text = fold(event.getTitle() + " " + event.getVenueName());
              for (String word : words) {
                if (!text.contains(word)) {
                  return false;
                }
              }
              return true;
            })
        .toList();
  }

  private static String fold(String text) {
    return Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
