package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.promo.DiscountType;
import com.sorinwork.tickets.promo.PromoCodeService;
import com.sorinwork.tickets.promo.PromoForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/events/{eventId}/promo-codes")
public class AdminPromoController {

  private final EventService events;
  private final PromoCodeService promos;

  public AdminPromoController(EventService events, PromoCodeService promos) {
    this.events = events;
    this.promos = promos;
  }

  @GetMapping
  public String list(@PathVariable long eventId, Model model) {
    model.addAttribute("form", new PromoForm());
    return render(eventId, model);
  }

  @PostMapping
  public String create(
      @PathVariable long eventId,
      @Valid @ModelAttribute("form") PromoForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      return render(eventId, model);
    }
    try {
      promos.create(eventId, form);
    } catch (BusinessException e) {
      binding.rejectValue("code", "duplicate", e.getMessage());
      return render(eventId, model);
    }
    redirect.addFlashAttribute("flash", "Promo code created.");
    return "redirect:/admin/events/" + eventId + "/promo-codes";
  }

  @PostMapping("/{promoId}/active")
  public String setActive(
      @PathVariable long eventId,
      @PathVariable long promoId,
      @RequestParam boolean active,
      RedirectAttributes redirect) {
    promos.setActive(eventId, promoId, active);
    redirect.addFlashAttribute("flash", active ? "Promo code enabled." : "Promo code disabled.");
    return "redirect:/admin/events/" + eventId + "/promo-codes";
  }

  private String render(long eventId, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("promos", promos.forEvent(eventId));
    model.addAttribute("types", DiscountType.values());
    return "admin/promos/list";
  }
}
