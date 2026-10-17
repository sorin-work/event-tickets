package com.sorinwork.tickets.web;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.order.CheckoutForm;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderItem;
import com.sorinwork.tickets.order.OrderStatus;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
    if (order.getStatus() == OrderStatus.PAID
        || (order.getStatus() == OrderStatus.CANCELLED && order.getPaidAt() != null)) {
      return "redirect:/orders/" + token;
    }
    model.addAttribute("form", new CheckoutForm());
    return render(order, model);
  }

  @PostMapping("/checkout/{token}")
  public String complete(
      @PathVariable String token,
      @Valid @ModelAttribute("form") CheckoutForm form,
      BindingResult binding,
      Model model) {
    Order order = checkout.getByToken(token);
    if (order.getStatus() == OrderStatus.PAID) {
      // A double click or a resubmitted form: the order is done, show the tickets.
      return "redirect:/orders/" + token;
    }
    if (binding.hasErrors()) {
      return render(order, model);
    }
    try {
      checkout.complete(token, form);
    } catch (ObjectOptimisticLockingFailureException e) {
      return "redirect:/orders/" + token;
    } catch (BusinessException e) {
      model.addAttribute("error", e.getMessage());
      return render(order, model);
    }
    return "redirect:/orders/" + token;
  }

  @PostMapping("/checkout/{token}/promo")
  public String applyPromo(
      @PathVariable String token,
      @RequestParam(defaultValue = "") String code,
      @RequestHeader(value = "HX-Request", required = false) String htmx,
      Model model,
      RedirectAttributes redirect) {
    try {
      checkout.applyPromoCode(token, code);
    } catch (BusinessException e) {
      if (htmx == null) {
        redirect.addFlashAttribute("promoError", e.getMessage());
      }
      model.addAttribute("promoError", e.getMessage());
    }
    return summary(token, htmx, model);
  }

  @PostMapping("/checkout/{token}/promo/remove")
  public String removePromo(
      @PathVariable String token,
      @RequestHeader(value = "HX-Request", required = false) String htmx,
      Model model) {
    try {
      checkout.removePromoCode(token);
    } catch (BusinessException e) {
      model.addAttribute("promoError", e.getMessage());
    }
    return summary(token, htmx, model);
  }

  /** htmx requests get the refreshed order summary; plain form posts go back to the page. */
  private String summary(String token, String htmx, Model model) {
    if (htmx == null) {
      return "redirect:/checkout/" + token;
    }
    Order order = checkout.getByToken(token);
    model.addAttribute("order", order);
    model.addAttribute("event", order.getEvent());
    model.addAttribute("holdActive", order.isHoldActive(clock.instant()));
    return "public/checkout :: summary";
  }

  @GetMapping("/orders/{token}")
  public String confirmation(@PathVariable String token, Model model) {
    Order order = checkout.getByToken(token);
    boolean cancelledAfterPayment =
        order.getStatus() == OrderStatus.CANCELLED && order.getPaidAt() != null;
    if (order.getStatus() != OrderStatus.PAID && !cancelledAfterPayment) {
      return "redirect:/checkout/" + token;
    }
    model.addAttribute("order", order);
    model.addAttribute("event", order.getEvent());
    model.addAttribute("tickets", checkout.ticketsOf(order));
    return "public/order";
  }

  private String render(Order order, Model model) {
    model.addAttribute("seats", seatLabels(order));
    model.addAttribute("order", order);
    model.addAttribute("event", order.getEvent());
    model.addAttribute("holdActive", order.isHoldActive(clock.instant()));
    return "public/checkout";
  }

  /** "Standing 1", "Standing 2", "VIP" ... in the order tickets are issued. */
  static List<String> seatLabels(Order order) {
    List<String> labels = new ArrayList<>();
    for (OrderItem item : order.getItems()) {
      for (int i = 1; i <= item.getQuantity(); i++) {
        String name = item.getTier().getName();
        labels.add(item.getQuantity() > 1 ? name + " " + i : name);
      }
    }
    return labels;
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
