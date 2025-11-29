package com.sorinwork.tickets.web;

import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.ticket.QrCodeService;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

  private final TicketRepository tickets;
  private final QrCodeService qrCodes;

  public TicketController(TicketRepository tickets, QrCodeService qrCodes) {
    this.tickets = tickets;
    this.qrCodes = qrCodes;
  }

  @GetMapping(value = "/tickets/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> qrCode(@PathVariable long id) {
    Ticket ticket = tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket"));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
        .body(qrCodes.png(ticket.getCode(), 240));
  }
}
