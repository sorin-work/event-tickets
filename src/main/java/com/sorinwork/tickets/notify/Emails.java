package com.sorinwork.tickets.notify;

import com.sorinwork.tickets.common.Money;
import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderItem;
import com.sorinwork.tickets.web.Formatters;
import org.springframework.stereotype.Component;

/** Plain-text emails sent to buyers. */
@Component
public class Emails {

  private final Notifier notifier;
  private final AppProperties properties;
  private final Formatters fmt;

  public Emails(Notifier notifier, AppProperties properties, Formatters fmt) {
    this.notifier = notifier;
    this.properties = properties;
    this.fmt = fmt;
  }

  public void orderConfirmation(Order order) {
    Event event = order.getEvent();
    StringBuilder body = new StringBuilder();
    body.append("Hi ").append(order.getBuyerName()).append(",\n\n");
    body.append("Thanks for your order ").append(order.getReference()).append(".\n\n");
    body.append(event.getTitle()).append('\n');
    body.append(fmt.when(event)).append(" (").append(event.getTimeZone()).append(")\n");
    body.append(event.getVenueName());
    if (event.getVenueAddress() != null) {
      body.append(", ").append(event.getVenueAddress());
    }
    body.append("\n\n");
    for (OrderItem item : order.getItems()) {
      body.append(item.getQuantity()).append(" × ").append(item.getTier().getName()).append('\n');
    }
    body.append("Total: ").append(Money.format(order.getTotalCents(), event.getCurrency()));
    body.append("\n\nYour tickets with their QR codes are here:\n");
    body.append(properties.url("/orders/" + order.getAccessToken())).append("\n\n");
    body.append("Show the QR code at the door, on your phone or printed.\n");
    notifier.send(
        order.getBuyerEmail(),
        "Your tickets for " + event.getTitle(),
        body.toString(),
        event.getId());
  }
}
