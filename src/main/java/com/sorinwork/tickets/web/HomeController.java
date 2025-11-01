package com.sorinwork.tickets.web;

import com.sorinwork.tickets.event.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  private final EventService events;

  public HomeController(EventService events) {
    this.events = events;
  }

  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("events", events.upcomingPublished());
    return "public/home";
  }
}
