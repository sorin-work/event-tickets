package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.report.SalesReportService;
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
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("report", reports.forEvent(eventId));
    return "admin/report/show";
  }
}
