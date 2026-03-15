package com.sorinwork.tickets.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class CheckoutForm {

  @NotBlank
  @Size(max = 120)
  private String name;

  @NotBlank
  @Email
  @Size(max = 255)
  private String email;

  /** Optional name per ticket, in the order the seats are listed. */
  private List<@Size(max = 120) String> attendeeNames = new ArrayList<>();

  public CheckoutForm() {}

  public CheckoutForm(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getAttendeeNames() {
    return attendeeNames;
  }

  public void setAttendeeNames(List<String> attendeeNames) {
    this.attendeeNames = attendeeNames == null ? new ArrayList<>() : attendeeNames;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
