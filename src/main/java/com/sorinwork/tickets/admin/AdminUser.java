package com.sorinwork.tickets.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_users")
public class AdminUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String email;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AdminUser() {}

  public AdminUser(String email, String displayName, String passwordHash, Instant createdAt) {
    this.email = normalizeEmail(email);
    this.displayName = displayName;
    this.passwordHash = passwordHash;
    this.createdAt = createdAt;
  }

  public static String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
