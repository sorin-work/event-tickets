package com.sorinwork.tickets.web;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.order.CheckoutForm;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderItem;
import com.sorinwork.tickets.order.OrderStatus;
import com.sorinwork.tickets.waitlist.WaitlistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

  /** Cookie with the browser's open checkout for an event, so starting again replaces it. */
  static final String HOLD_COOKIE_PREFIX = "hold_";

  private final EventService events;
  private final CheckoutService checkout;
  private final WaitlistService waitlist;
  private final AppProperties properties;
  private final Clock clock;

  public CheckoutController(
      EventService events,
      CheckoutService checkout,
      WaitlistService waitlist,
      AppProperties properties,
      Clock clock) {
    this.events = events;
    this.checkout = checkout;
    this.waitlist = waitlist;
    this.properties = properties;
    this.clock = clock;
  }

  @PostMapping("/events/{slug}/checkout")
  public String start(
      @PathVariable String slug,
      @RequestParam Map<String, String> params,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes redirect) {
    Event event = events.getPublic(slug);
    try {
      Map<Long, Integer> selection = quantities(params);
      String previous = previousHold(request, event.getId());
      Order order = retryOnce(() -> checkout.start(event.getId(), selection, previous));
      setHoldCookie(
          request,
          response,
          event.getId(),
          order.getAccessToken(),
          properties.checkout().holdDuration());
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
      // Reload: the failed attempt may have changed the order, e.g. removed a withdrawn discount.
      return render(checkout.getByToken(token), model);
    }
    return "redirect:/orders/" + token;
  }

  /** The buyer gives the seats back, or declines a waitlist offer. */
  @PostMapping("/checkout/{token}/release")
  public String release(
      @PathVariable String token,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes redirect) {
    Order order = checkout.getByToken(token);
    boolean offer = waitlist.isOffer(order.getId());
    boolean ranOut = !order.isHoldActive(clock.instant());
    try {
      order = retryOnce(() -> checkout.release(token));
    } catch (ObjectOptimisticLockingFailureException e) {
      // Paid or expired at the same moment; the checkout page shows which.
      return "redirect:/checkout/" + token;
    }
    if (order.getStatus() != OrderStatus.EXPIRED) {
      return "redirect:/checkout/" + token;
    }
    setHoldCookie(request, response, order.getEvent().getId(), "", Duration.ZERO);
    redirect.addFlashAttribute(
        "notice",
        ranOut
            ? "Your reservation had already run out and the tickets were released."
            : offer
                ? "You declined the offer. The tickets go to the next person on the waitlist."
                : "Your tickets have been given back. You can choose again.");
    return "redirect:/events/" + order.getEvent().getSlug();
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
    model.addAttribute("offer", waitlist.isOffer(order.getId()));
    return "public/checkout";
  }

  /**
   * Runs a checkout change again once when the database picked it as a deadlock victim or could not
   * get a lock in time. The lock order makes this rare; one retry turns it into a short wait
   * instead of an error page.
   */
  private static <T> T retryOnce(Supplier<T> change) {
    try {
      return change.get();
    } catch (PessimisticLockingFailureException e) {
      return change.get();
    }
  }

  private static String previousHold(HttpServletRequest request, long eventId) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals(HOLD_COOKIE_PREFIX + eventId) && !cookie.getValue().isBlank()) {
        return cookie.getValue();
      }
    }
    return null;
  }

  /** Remembers (or with an empty token and zero lifetime, forgets) the browser's open checkout. */
  private static void setHoldCookie(
      HttpServletRequest request,
      HttpServletResponse response,
      long eventId,
      String token,
      Duration lifetime) {
    ResponseCookie cookie =
        ResponseCookie.from(HOLD_COOKIE_PREFIX + eventId, token)
            .path("/")
            .httpOnly(true)
            .secure(request.isSecure())
            .sameSite("Lax")
            .maxAge(lifetime)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
