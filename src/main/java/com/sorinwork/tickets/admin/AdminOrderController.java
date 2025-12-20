package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminOrderController {

  private final EventService events;
  private final OrderAdminService orders;

  public AdminOrderController(EventService events, OrderAdminService orders) {
    this.events = events;
    this.orders = orders;
  }

  @GetMapping("/admin/events/{eventId}/orders")
  public String list(@PathVariable long eventId, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("orders", orders.forEvent(eventId));
    return "admin/orders/list";
  }

  @GetMapping("/admin/orders/{orderId}")
  public String show(@PathVariable long orderId, Model model) {
    Order order = orders.get(orderId);
    model.addAttribute("order", order);
    model.addAttribute("event", order.getEvent());
    model.addAttribute("tickets", orders.tickets(orderId));
    return "admin/orders/show";
  }

  @PostMapping("/admin/orders/{orderId}/cancel")
  public String cancel(@PathVariable long orderId, RedirectAttributes redirect) {
    try {
      orders.cancel(orderId);
      redirect.addFlashAttribute("flash", "Order cancelled. Its tickets are no longer valid.");
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/orders/" + orderId;
  }
}
