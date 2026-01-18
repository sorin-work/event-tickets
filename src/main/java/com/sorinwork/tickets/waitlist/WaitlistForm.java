package com.sorinwork.tickets.waitlist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class WaitlistForm {

  @NotNull private Long tierId;

  @NotBlank
  @Size(max = 120)
  private String name;

  @NotBlank
  @Email
  @Size(max = 255)
  private String email;

  @NotNull
  @Min(1)
  @Max(50)
  private Integer quantity = 1;

  public WaitlistForm() {}

  public WaitlistForm(Long tierId, String name, String email, Integer quantity) {
    this.tierId = tierId;
    this.name = name;
    this.email = email;
    this.quantity = quantity;
  }

  public Long getTierId() {
    return tierId;
  }

  public void setTierId(Long tierId) {
    this.tierId = tierId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }
}
