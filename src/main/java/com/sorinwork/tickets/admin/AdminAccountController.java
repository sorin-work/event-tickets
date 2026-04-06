package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.BusinessException;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/account")
public class AdminAccountController {

  private final AdminAccountService accounts;

  public AdminAccountController(AdminAccountService accounts) {
    this.accounts = accounts;
  }

  @GetMapping
  public String page() {
    return "admin/users/account";
  }

  @PostMapping("/password")
  public String changePassword(
      @RequestParam String currentPassword,
      @RequestParam String newPassword,
      @RequestParam String confirmation,
      Principal principal,
      RedirectAttributes redirect) {
    try {
      accounts.changePassword(principal.getName(), currentPassword, newPassword, confirmation);
      redirect.addFlashAttribute("flash", "Password changed.");
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/account";
  }
}
