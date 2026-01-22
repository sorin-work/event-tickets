package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.waitlist.WaitlistEntry;
import com.sorinwork.tickets.waitlist.WaitlistService;
import com.sorinwork.tickets.waitlist.WaitlistStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminWaitlistController {

  private final EventService events;
  private final WaitlistService waitlist;

  public AdminWaitlistController(EventService events, WaitlistService waitlist) {
    this.events = events;
    this.waitlist = waitlist;
  }

  @GetMapping("/admin/events/{eventId}/waitlist")
  public String list(@PathVariable long eventId, Model model) {
    List<WaitlistEntry> entries = waitlist.forEvent(eventId);
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("entries", entries);
    model.addAttribute("positions", positions(entries));
    return "admin/waitlist/list";
  }

  @PostMapping("/admin/events/{eventId}/waitlist/{entryId}/remove")
  public String remove(
      @PathVariable long eventId, @PathVariable long entryId, RedirectAttributes redirect) {
    try {
      WaitlistEntry entry = waitlist.remove(eventId, entryId);
      redirect.addFlashAttribute("flash", entry.getName() + " was removed from the waitlist.");
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/events/" + eventId + "/waitlist";
  }

  /** Queue position of each waiting entry within its tier, 1 = next to get an offer. */
  static Map<Long, Integer> positions(List<WaitlistEntry> entries) {
    Map<Long, Integer> perTier = new HashMap<>();
    Map<Long, Integer> positions = new HashMap<>();
    for (WaitlistEntry entry : entries) {
      if (entry.getStatus() == WaitlistStatus.WAITING) {
        int position = perTier.merge(entry.getTier().getId(), 1, Integer::sum);
        positions.put(entry.getId(), position);
      }
    }
    return positions;
  }
}
