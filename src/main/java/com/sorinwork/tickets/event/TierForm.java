package com.sorinwork.tickets.event;

import com.sorinwork.tickets.common.Money;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class TierForm {

  @NotBlank
  @Size(max = 80)
  private String name;

  @Size(max = 500)
  private String description;

  @NotBlank private String price = "0";

  @NotNull
  @Min(0)
  @Max(100000)
  private Integer quantity;

  @NotNull
  @Min(1)
  @Max(50)
  private Integer maxPerOrder = 10;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime salesStart;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime salesEnd;

  private boolean paused;

  public static TierForm from(TicketTier tier) {
    TierForm form = new TierForm();
    form.name = tier.getName();
    form.description = tier.getDescription();
    form.price = Money.toDecimal(tier.getPriceCents());
    form.quantity = tier.getQuantity();
    form.maxPerOrder = tier.getMaxPerOrder();
    form.salesStart = tier.getSalesStart();
    form.salesEnd = tier.getSalesEnd();
    form.paused = tier.isPaused();
    return form;
  }

  public long priceCents() {
    return Money.parse(price);
  }

  @AssertTrue(message = "must be an amount like 12.50")
  public boolean isPriceValid() {
    if (price == null || price.isBlank()) {
      return true;
    }
    try {
      Money.parse(price);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @AssertTrue(message = "must be after the start of sales")
  public boolean isSalesWindowValid() {
    return salesStart == null || salesEnd == null || salesEnd.isAfter(salesStart);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getPrice() {
    return price;
  }

  public void setPrice(String price) {
    this.price = price;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public Integer getMaxPerOrder() {
    return maxPerOrder;
  }

  public void setMaxPerOrder(Integer maxPerOrder) {
    this.maxPerOrder = maxPerOrder;
  }

  public LocalDateTime getSalesStart() {
    return salesStart;
  }

  public void setSalesStart(LocalDateTime salesStart) {
    this.salesStart = salesStart;
  }

  public LocalDateTime getSalesEnd() {
    return salesEnd;
  }

  public void setSalesEnd(LocalDateTime salesEnd) {
    this.salesEnd = salesEnd;
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }
}
