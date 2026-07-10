package com.sorinwork.tickets.notify;

import com.sorinwork.tickets.common.Money;
import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderItem;
import com.sorinwork.tickets.waitlist.WaitlistEntry;
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

  /** One email listing the ticket links of all upcoming orders of a buyer. */
  public void ticketLinks(String email, java.util.List<Order> orders) {
    StringBuilder body = new StringBuilder("Hi,\n\nHere are your tickets for upcoming events:\n\n");
    for (Order order : orders) {
      Event event = order.getEvent();
      body.append(event.getTitle()).append(" – ").append(fmt.when(event)).append('\n');
      body.append(properties.url("/orders/" + order.getAccessToken())).append("\n\n");
    }
    body.append("If you did not ask for this email you can ignore it.\n");
    notifier.send(email, "Your tickets", body.toString(), null);
  }

  public void reminder(Order order) {
    Event event = order.getEvent();
    String body =
        "Hi "
            + order.getBuyerName()
            + ",\n\nSee you soon at "
            + event.getTitle()
            + ": "
            + fmt.when(event)
            + " ("
            + event.getTimeZone()
            + ") at "
            + event.getVenueName()
            + (event.getVenueAddress() == null ? "" : ", " + event.getVenueAddress())
            + ".\n\nHave your QR codes ready at the door:\n"
            + properties.url("/orders/" + order.getAccessToken())
            + "\n";
    notifier.send(order.getBuyerEmail(), "Tomorrow: " + event.getTitle(), body, event.getId());
  }

  public void orderCancelled(Order order) {
    Event event = order.getEvent();
    String body =
        "Hi "
            + order.getBuyerName()
            + ",\n\nYour order "
            + order.getReference()
            + " for "
            + event.getTitle()
            + " ("
            + fmt.when(event)
            + ") has been cancelled and its tickets are no longer valid.\n\n"
            + "If you paid for it, the organiser will be in touch about a refund.\n";
    notifier.send(
        order.getBuyerEmail(), "Order cancelled: " + event.getTitle(), body, event.getId());
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

  public void waitlistJoined(WaitlistEntry entry) {
    Event event = entry.getEvent();
    String body =
        "Hi "
            + entry.getName()
            + ",\n\nYou are on the waitlist for "
            + entry.getQuantity()
            + " × "
            + entry.getTier().getName()
            + " for "
            + event.getTitle()
            + ". If tickets are released we offer them in the order people joined and"
            + " email you a link to buy them.\n\nChanged your mind? Leave the waitlist here:\n"
            + properties.url("/waitlist/leave/" + entry.getLeaveToken())
            + "\n";
    notifier.send(
        entry.getEmail(), "You are on the waitlist: " + event.getTitle(), body, event.getId());
  }

  /** Released tickets are held for someone on the waitlist; tells them how to buy them. */
  public void waitlistOffer(WaitlistEntry entry, Order hold) {
    Event event = hold.getEvent();
    String body =
        "Hi "
            + entry.getName()
            + ",\n\nGood news: "
            + entry.getQuantity()
            + " × "
            + entry.getTier().getName()
            + " for "
            + event.getTitle()
            + " became available and we are holding them for you until "
            + hold.getHoldExpiresAt().atZone(event.zone()).toLocalDateTime().withSecond(0)
            + " ("
            + event.getTimeZone()
            + ").\n\nComplete your order here:\n"
            + properties.url("/checkout/" + hold.getAccessToken())
            + "\n\nIf you no longer want them, just ignore this email and they will go to the"
            + " next person on the list.\n";
    notifier.send(entry.getEmail(), "Tickets available: " + event.getTitle(), body, event.getId());
  }
}
