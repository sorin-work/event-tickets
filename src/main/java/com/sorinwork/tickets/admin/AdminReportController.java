package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.report.DailySales;
import com.sorinwork.tickets.report.SalesReportService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminReportController {

  private final EventService events;
  private final SalesReportService reports;

  public AdminReportController(EventService events, SalesReportService reports) {
    this.events = events;
    this.reports = reports;
  }

  @GetMapping("/admin/events/{eventId}/report")
  public String report(@PathVariable long eventId, Model model) {
    Event event = events.get(eventId);
    List<DailySales> days = reports.dailySales(event);
    model.addAttribute("event", event);
    model.addAttribute("report", reports.forEvent(eventId));
    model.addAttribute("days", days);
    model.addAttribute("promos", reports.promoUsage(eventId));
    model.addAttribute("maxDay", days.stream().mapToLong(DailySales::tickets).max().orElse(1));
    return "admin/report/show";
  }
}
