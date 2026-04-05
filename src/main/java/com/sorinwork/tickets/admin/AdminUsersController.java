package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.BusinessException;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUsersController {

  private final AdminAccountService accounts;

  public AdminUsersController(AdminAccountService accounts) {
    this.accounts = accounts;
  }

  @GetMapping
  public String list(Model model, Principal principal) {
    model.addAttribute("users", accounts.list());
    model.addAttribute("me", principal.getName());
    return "admin/users/list";
  }

  @PostMapping
  public String create(
      @RequestParam String email,
      @RequestParam String name,
      @RequestParam String password,
      RedirectAttributes redirect) {
    try {
      AdminUser user = accounts.create(email, name, password);
      redirect.addFlashAttribute("flash", user.getEmail() + " can now sign in.");
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/users";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable long id, Principal principal, RedirectAttributes redirect) {
    try {
      accounts.delete(id, principal.getName());
      redirect.addFlashAttribute("flash", "Admin removed.");
    } catch (BusinessException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/users";
  }
}
