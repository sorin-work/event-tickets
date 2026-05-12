package com.sorinwork.tickets.web;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TierService;
import com.sorinwork.tickets.waitlist.WaitlistEntry;
import com.sorinwork.tickets.waitlist.WaitlistForm;
import com.sorinwork.tickets.waitlist.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WaitlistController {

  private final EventService events;
  private final TierService tiers;
  private final WaitlistService waitlist;

  public WaitlistController(EventService events, TierService tiers, WaitlistService waitlist) {
    this.events = events;
    this.tiers = tiers;
    this.waitlist = waitlist;
  }

  @GetMapping("/events/{slug}/waitlist")
  public String form(@PathVariable String slug, @RequestParam long tier, Model model) {
    WaitlistForm form = new WaitlistForm();
    form.setTierId(tier);
    model.addAttribute("form", form);
    return render(events.getPublic(slug), tier, model);
  }

  @PostMapping("/events/{slug}/waitlist")
  public String join(
      @PathVariable String slug,
      @Valid @ModelAttribute("form") WaitlistForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    Event event = events.getPublic(slug);
    if (binding.hasErrors()) {
      return render(event, form.getTierId(), model);
    }
    WaitlistEntry entry;
    try {
      entry = waitlist.join(event, form);
    } catch (BusinessException e) {
      model.addAttribute("error", e.getMessage());
      return render(event, form.getTierId(), model);
    }
    redirect.addFlashAttribute(
        "notice",
        "You are number "
            + waitlist.position(entry)
            + " on the waitlist. We will email you if tickets become available.");
    return "redirect:/events/" + slug;
  }

  private String render(Event event, Long tierId, Model model) {
    model.addAttribute("event", event);
    if (tierId != null) {
      model.addAttribute("tier", tiers.get(event.getId(), tierId));
    }
    return "public/waitlist";
  }
}
