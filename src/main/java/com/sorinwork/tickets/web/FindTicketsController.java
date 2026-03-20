package com.sorinwork.tickets.web;

import com.sorinwork.tickets.order.TicketLookupService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FindTicketsController {

  private final TicketLookupService lookup;

  public FindTicketsController(TicketLookupService lookup) {
    this.lookup = lookup;
  }

  @GetMapping("/tickets/find")
  public String form() {
    return "public/find-tickets";
  }

  @PostMapping("/tickets/find")
  public String send(@RequestParam(defaultValue = "") @NotBlank @Email String email, Model model) {
    if (email.isBlank() || !email.contains("@")) {
      model.addAttribute("error", "Enter the email address you used to buy the tickets.");
      return "public/find-tickets";
    }
    lookup.sendLinks(email);
    model.addAttribute("sent", true);
    return "public/find-tickets";
  }
}
