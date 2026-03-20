package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.ticket.CheckInService;
import com.sorinwork.tickets.ticket.Ticket;
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
  private final CheckInService checkIn;

  public AdminOrderController(
      EventService events, OrderAdminService orders, CheckInService checkIn) {
    this.events = events;
    this.orders = orders;
    this.checkIn = checkIn;
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

  @PostMapping("/admin/tickets/{ticketId}/name")
  public String rename(
      @PathVariable long ticketId,
      @org.springframework.web.bind.annotation.RequestParam String name,
      RedirectAttributes redirect) {
    try {
      Ticket ticket = orders.renameTicket(ticketId, name);
      redirect.addFlashAttribute(
          "flash", "Ticket " + ticket.getCode() + " is now for " + ticket.getAttendeeName() + ".");
      return "redirect:/admin/orders/" + ticket.getOrder().getId();
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/orders/" + orders.ticketOrderId(ticketId);
    }
  }

  @PostMapping("/admin/orders/{orderId}/resend")
  public String resend(@PathVariable long orderId, RedirectAttributes redirect) {
    try {
      Order order = orders.resendConfirmation(orderId);
      redirect.addFlashAttribute("flash", "Tickets sent again to " + order.getBuyerEmail() + ".");
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/orders/" + orderId;
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

  @PostMapping("/admin/tickets/{ticketId}/undo-check-in")
  public String undoCheckIn(@PathVariable long ticketId, RedirectAttributes redirect) {
    Ticket ticket = checkIn.undo(ticketId);
    redirect.addFlashAttribute("flash", "Check-in undone for " + ticket.getCode() + ".");
    return "redirect:/admin/orders/" + ticket.getOrder().getId();
  }
}
