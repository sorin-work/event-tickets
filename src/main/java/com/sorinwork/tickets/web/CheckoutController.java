package com.sorinwork.tickets.web;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CheckoutController {

  static final String QUANTITY_PREFIX = "qty_";

  private final EventService events;
  private final CheckoutService checkout;
  private final Clock clock;

  public CheckoutController(EventService events, CheckoutService checkout, Clock clock) {
    this.events = events;
    this.checkout = checkout;
    this.clock = clock;
  }

  @PostMapping("/events/{slug}/checkout")
  public String start(
      @PathVariable String slug,
      @RequestParam Map<String, String> params,
      RedirectAttributes redirect) {
    Event event = events.getPublic(slug);
    try {
      Order order = checkout.start(event.getId(), quantities(params));
      return "redirect:/checkout/" + order.getAccessToken();
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
      return "redirect:/events/" + slug;
    }
  }

  @GetMapping("/checkout/{token}")
  public String show(@PathVariable String token, Model model) {
    Order order = checkout.getByToken(token);
    model.addAttribute("order", order);
    model.addAttribute("event", order.getEvent());
    model.addAttribute("holdActive", order.isHoldActive(clock.instant()));
    return "public/checkout";
  }

  static Map<Long, Integer> quantities(Map<String, String> params) {
    Map<Long, Integer> result = new LinkedHashMap<>();
    params.forEach(
        (name, value) -> {
          if (name.startsWith(QUANTITY_PREFIX) && value != null && !value.isBlank()) {
            try {
              result.put(
                  Long.parseLong(name.substring(QUANTITY_PREFIX.length())),
                  Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
              throw new BusinessException("Please enter whole numbers of tickets.");
            }
          }
        });
    return result;
  }
}
