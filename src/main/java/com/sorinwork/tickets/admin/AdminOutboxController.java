package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.notify.OutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminOutboxController {

  static final int LIMIT = 100;

  private final OutboxRepository outbox;

  public AdminOutboxController(OutboxRepository outbox) {
    this.outbox = outbox;
  }

  @GetMapping("/admin/outbox")
  public String list(
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "") String to,
      Model model) {
    PageRequest limit = PageRequest.of(0, LIMIT);
    model.addAttribute(
        "messages",
        to.isBlank()
            ? outbox.findByOrderByCreatedAtDescIdDesc(limit)
            : outbox.findByRecipientContainingIgnoreCaseOrderByCreatedAtDescIdDesc(
                to.trim(), limit));
    model.addAttribute("to", to);
    model.addAttribute("limit", LIMIT);
    return "admin/outbox/list";
  }
}
