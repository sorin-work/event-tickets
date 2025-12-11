package com.sorinwork.tickets.promo;

import com.sorinwork.tickets.common.Money;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class PromoForm {

  @NotBlank
  @Pattern(regexp = "\\s*[A-Za-z0-9_-]{3,32}\\s*", message = "use 3-32 letters, digits, - or _")
  private String code;

  @NotNull private DiscountType discountType = DiscountType.PERCENT;

  @NotBlank private String amount;

  @Min(1)
  private Integer maxUses;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime validUntil;

  /** Percent as a whole number, fixed discounts in cents. */
  public long amountValue() {
    if (discountType == DiscountType.PERCENT) {
      return Long.parseLong(amount.trim().replace("%", ""));
    }
    return Money.parse(amount);
  }

  @AssertTrue(message = "must be 1-100 for a percentage or an amount like 5.00")
  public boolean isAmountValid() {
    if (amount == null || amount.isBlank() || discountType == null) {
      return true;
    }
    try {
      long value = amountValue();
      return discountType == DiscountType.PERCENT ? value >= 1 && value <= 100 : value > 0;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public DiscountType getDiscountType() {
    return discountType;
  }

  public void setDiscountType(DiscountType discountType) {
    this.discountType = discountType;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public Integer getMaxUses() {
    return maxUses;
  }

  public void setMaxUses(Integer maxUses) {
    this.maxUses = maxUses;
  }

  public LocalDateTime getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(LocalDateTime validUntil) {
    this.validUntil = validUntil;
  }
}
