package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.Csv;
import com.sorinwork.tickets.common.Money;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.TierAvailability;
import com.sorinwork.tickets.report.DailySales;
import com.sorinwork.tickets.report.SalesReport;
import com.sorinwork.tickets.report.SalesReportService;
import com.sorinwork.tickets.waitlist.WaitlistService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminReportController {

  private final EventService events;
  private final SalesReportService reports;
  private final AvailabilityService availability;
  private final WaitlistService waitlist;

  public AdminReportController(
      EventService events,
      SalesReportService reports,
      AvailabilityService availability,
      WaitlistService waitlist) {
    this.events = events;
    this.reports = reports;
    this.availability = availability;
    this.waitlist = waitlist;
  }

  @GetMapping("/admin/events/{eventId}/report")
  public String report(@PathVariable long eventId, Model model) {
    Event event = events.get(eventId);
    List<DailySales> days = reports.dailySales(event);
    model.addAttribute("event", event);
    model.addAttribute("report", reports.forEvent(eventId));
    model.addAttribute("days", days);
    model.addAttribute("promos", reports.promoUsage(eventId));
    model.addAttribute("checkIns", reports.checkInsByHour(event));
    model.addAttribute(
        "held",
        availability.forEvent(eventId).values().stream().mapToInt(TierAvailability::held).sum());
    model.addAttribute(
        "waiting",
        waitlist.waitingPerTier(eventId).values().stream().mapToLong(Long::longValue).sum());
    model.addAttribute("maxDay", days.stream().mapToLong(DailySales::tickets).max().orElse(1));
    return "admin/report/show";
  }

  @GetMapping("/admin/events/{eventId}/report.csv")
  public void exportCsv(@PathVariable long eventId, HttpServletResponse response)
      throws IOException {
    Event event = events.get(eventId);
    SalesReport report = reports.forEvent(eventId);
    response.setContentType("text/csv;charset=UTF-8");
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment()
            .filename(event.getSlug() + "-sales.csv", StandardCharsets.UTF_8)
            .build()
            .toString());
    Csv csv = new Csv(response.getWriter());
    csv.row("Section", "Name", "Price", "Sold", "Capacity", "Amount (" + event.getCurrency() + ")");
    for (SalesReport.TierLine line : report.tiers()) {
      csv.row(
          "Tier",
          line.name(),
          Money.toDecimal(line.priceCents()),
          line.sold(),
          line.capacity(),
          Money.toDecimal(line.grossCents()));
    }
    csv.row(
        "Total",
        "Gross",
        "",
        report.ticketsSold(),
        report.capacity(),
        Money.toDecimal(report.grossCents()));
    csv.row("Total", "Discounts", "", "", "", Money.toDecimal(report.discountCents()));
    csv.row("Total", "Net", "", "", "", Money.toDecimal(report.netCents()));
    for (DailySales day : reports.dailySales(event)) {
      csv.row("Day", day.date(), "", day.tickets(), "", Money.toDecimal(day.netCents()));
    }
  }
}
