package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminAccountService {

  static final int MIN_PASSWORD_LENGTH = 10;

  private final AdminUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public AdminAccountService(
      AdminUserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AdminUser> list() {
    return users.findAll(Sort.by("email"));
  }

  public AdminUser create(String email, String name, String password) {
    String normalized = AdminUser.normalizeEmail(email);
    if (normalized == null || !normalized.contains("@")) {
      throw new BusinessException("Enter a valid email address.");
    }
    if (name == null || name.isBlank()) {
      throw new BusinessException("Enter a name.");
    }
    checkPassword(password);
    if (users.existsByEmail(normalized)) {
      throw new BusinessException("There is already an admin with that email.");
    }
    return users.save(
        new AdminUser(normalized, name.trim(), passwordEncoder.encode(password), clock.instant()));
  }

  /** Removes an admin. Nobody can remove themselves, so there is always one left. */
  public void delete(long id, String currentEmail) {
    AdminUser user = users.findById(id).orElseThrow(() -> new NotFoundException("Admin " + id));
    if (user.getEmail().equals(AdminUser.normalizeEmail(currentEmail))) {
      throw new BusinessException("You cannot remove your own account.");
    }
    users.delete(user);
  }

  public void changePassword(
      String email, String currentPassword, String newPassword, String confirmation) {
    AdminUser user =
        users
            .findByEmail(AdminUser.normalizeEmail(email))
            .orElseThrow(() -> new NotFoundException("Admin " + email));
    if (currentPassword == null
        || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new BusinessException("Your current password is not right.");
    }
    checkPassword(newPassword);
    if (!newPassword.equals(confirmation)) {
      throw new BusinessException("The new passwords do not match.");
    }
    user.setPasswordHash(passwordEncoder.encode(newPassword));
  }

  static void checkPassword(String password) {
    if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
      throw new BusinessException(
          "Passwords need at least " + MIN_PASSWORD_LENGTH + " characters.");
    }
  }
}
