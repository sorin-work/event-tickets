package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventCopier;
import com.sorinwork.tickets.event.EventForm;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TierService;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.ticket.AttendeeService;
import com.sorinwork.tickets.waitlist.WaitlistService;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/events")
public class AdminEventController {

  static final List<String> TIME_ZONES =
      ZoneId.getAvailableZoneIds().stream()
          .filter(z -> z.contains("/") && !z.startsWith("Etc") && !z.startsWith("SystemV"))
          .sorted()
          .toList();

  private final EventService events;
  private final TierService tiers;
  private final AttendeeService attendees;
  private final EventCopier copier;
  private final AvailabilityService availability;
  private final AppProperties properties;
  private final WaitlistService waitlist;

  public AdminEventController(
      EventService events,
      TierService tiers,
      AttendeeService attendees,
      EventCopier copier,
      AvailabilityService availability,
      AppProperties properties,
      WaitlistService waitlist) {
    this.events = events;
    this.tiers = tiers;
    this.attendees = attendees;
    this.copier = copier;
    this.availability = availability;
    this.properties = properties;
    this.waitlist = waitlist;
  }

  @ModelAttribute("timeZones")
  List<String> timeZones() {
    return TIME_ZONES;
  }

  @GetMapping
  public String list(Model model) {
    model.addAttribute("events", events.listAll());
    model.addAttribute("sold", attendees.soldPerEvent());
    return "admin/events/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("form", new EventForm());
    return "admin/events/form";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("form") EventForm form,
      BindingResult binding,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      return "admin/events/form";
    }
    Event event = events.create(form);
    redirect.addFlashAttribute("flash", "Event created.");
    return "redirect:/admin/events/" + event.getId() + "/edit";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable long id, Model model) {
    Event event = events.get(id);
    model.addAttribute("event", event);
    model.addAttribute("publicUrl", properties.url("/events/" + event.getSlug()));
    model.addAttribute("tiers", tiers.forEvent(id));
    model.addAttribute("availability", availability.forEvent(id));
    model.addAttribute("waiting", waitlist.waitingPerTier(id));
    model.addAttribute("form", EventForm.from(event));
    return "admin/events/form";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable long id,
      @Valid @ModelAttribute("form") EventForm form,
      BindingResult binding,
      Model model,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      Event event = events.get(id);
      model.addAttribute("event", event);
      model.addAttribute("publicUrl", properties.url("/events/" + event.getSlug()));
      model.addAttribute("tiers", tiers.forEvent(id));
      model.addAttribute("availability", availability.forEvent(id));
      model.addAttribute("waiting", waitlist.waitingPerTier(id));
      return "admin/events/form";
    }
    events.update(id, form);
    redirect.addFlashAttribute("flash", "Event saved.");
    return "redirect:/admin/events/" + id + "/edit";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable long id, RedirectAttributes redirect) {
    try {
      events.delete(id);
      redirect.addFlashAttribute("flash", "Event deleted.");
      return "redirect:/admin/events";
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
      return "redirect:/admin/events/" + id + "/edit";
    }
  }

  @PostMapping("/{id}/duplicate")
  public String duplicate(@PathVariable long id, RedirectAttributes redirect) {
    Event copy = copier.duplicate(id);
    redirect.addFlashAttribute("flash", "Event copied. Set the new date and publish when ready.");
    return "redirect:/admin/events/" + copy.getId() + "/edit";
  }

  @PostMapping("/{id}/publish")
  public String publish(@PathVariable long id, RedirectAttributes redirect) {
    return changeStatus(id, redirect, () -> events.publish(id), "Event published.");
  }

  @PostMapping("/{id}/unpublish")
  public String unpublish(@PathVariable long id, RedirectAttributes redirect) {
    return changeStatus(id, redirect, () -> events.unpublish(id), "Event moved back to draft.");
  }

  @PostMapping("/{id}/cancel")
  public String cancel(@PathVariable long id, RedirectAttributes redirect) {
    return changeStatus(
        id,
        redirect,
        () -> events.cancel(id),
        "Event cancelled. Its orders were cancelled and buyers have been emailed.");
  }

  private String changeStatus(
      long id, RedirectAttributes redirect, Runnable action, String message) {
    try {
      action.run();
      redirect.addFlashAttribute("flash", message);
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/events/" + id + "/edit";
  }
}
