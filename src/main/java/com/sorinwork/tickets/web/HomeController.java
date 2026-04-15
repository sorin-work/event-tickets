package com.sorinwork.tickets.web;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

  private final EventService events;

  public HomeController(EventService events) {
    this.events = events;
  }

  @GetMapping("/")
  public String home(@RequestParam(defaultValue = "") String q, Model model) {
    List<Event> upcoming = events.upcomingPublished();
    model.addAttribute("q", q);
    model.addAttribute("hasEvents", !upcoming.isEmpty());
    model.addAttribute("events", matching(upcoming, q));
    return "public/home";
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
