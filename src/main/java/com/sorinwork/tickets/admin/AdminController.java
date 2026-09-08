package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.order.OrderAdminService;
import java.time.Duration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

  private final EventService events;
  private final OrderAdminService orders;

  public AdminController(EventService events, OrderAdminService orders) {
    this.events = events;
    this.orders = orders;
  }

  @GetMapping("/login")
  public String login() {
    return "admin/login";
  }

  @GetMapping
  public String dashboard(Model model) {
    model.addAttribute("upcoming", events.upcomingForAdmin());
    model.addAttribute("recentOrders", orders.recentPaid(10));
    model.addAttribute("ordersThisWeek", orders.paidSince(Duration.ofDays(7)));
    model.addAttribute("ticketsToday", orders.ticketsSoldSince(Duration.ofHours(24)));
    return "admin/dashboard";
  }
}
