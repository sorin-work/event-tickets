package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TierForm;
import com.sorinwork.tickets.event.TierService;
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
@RequestMapping("/admin/events/{eventId}/tiers")
public class AdminTierController {

  private final EventService events;
  private final TierService tiers;

  public AdminTierController(EventService events, TierService tiers) {
    this.events = events;
    this.tiers = tiers;
  }

  @GetMapping("/new")
  public String newForm(@PathVariable long eventId, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("form", new TierForm());
    return "admin/tiers/form";
  }

  @PostMapping
  public String create(
      @PathVariable long eventId,
      @Valid @ModelAttribute("form") TierForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      model.addAttribute("event", events.get(eventId));
      return "admin/tiers/form";
    }
    tiers.create(eventId, form);
    redirect.addFlashAttribute("flash", "Ticket tier added.");
    return "redirect:/admin/events/" + eventId + "/edit";
  }

  @GetMapping("/{tierId}/edit")
  public String editForm(@PathVariable long eventId, @PathVariable long tierId, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("tier", tiers.get(eventId, tierId));
    model.addAttribute("form", TierForm.from(tiers.get(eventId, tierId)));
    return "admin/tiers/form";
  }

  @PostMapping("/{tierId}")
  public String update(
      @PathVariable long eventId,
      @PathVariable long tierId,
      @Valid @ModelAttribute("form") TierForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      model.addAttribute("event", events.get(eventId));
      model.addAttribute("tier", tiers.get(eventId, tierId));
      return "admin/tiers/form";
    }
    tiers.update(eventId, tierId, form);
    redirect.addFlashAttribute("flash", "Ticket tier saved.");
    return "redirect:/admin/events/" + eventId + "/edit";
  }

  @PostMapping("/{tierId}/move")
  public String move(
      @PathVariable long eventId, @PathVariable long tierId, @RequestParam int direction) {
    tiers.move(eventId, tierId, direction);
    return "redirect:/admin/events/" + eventId + "/edit";
  }

  @PostMapping("/{tierId}/delete")
  public String delete(
      @PathVariable long eventId, @PathVariable long tierId, RedirectAttributes redirect) {
    tiers.delete(eventId, tierId);
    redirect.addFlashAttribute("flash", "Ticket tier removed.");
    return "redirect:/admin/events/" + eventId + "/edit";
  }
}
