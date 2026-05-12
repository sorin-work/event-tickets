package com.sorinwork.tickets.web;

import com.sorinwork.tickets.waitlist.WaitlistEntry;
import com.sorinwork.tickets.waitlist.WaitlistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class WaitlistLeaveController {

  private final WaitlistService waitlist;

  public WaitlistLeaveController(WaitlistService waitlist) {
    this.waitlist = waitlist;
  }

  @GetMapping("/waitlist/leave/{token}")
  public String confirm(@PathVariable String token, Model model) {
    model.addAttribute("entry", waitlist.byLeaveToken(token));
    return "public/waitlist-leave";
  }

  @PostMapping("/waitlist/leave/{token}")
  public String leave(@PathVariable String token, Model model) {
    WaitlistEntry entry = waitlist.leave(token);
    model.addAttribute("entry", entry);
    model.addAttribute("done", true);
    return "public/waitlist-leave";
  }
}
