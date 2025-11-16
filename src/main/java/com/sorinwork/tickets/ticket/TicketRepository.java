package com.sorinwork.tickets.ticket;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

  Optional<Ticket> findByCode(String code);

  @EntityGraph(attributePaths = "tier")
  List<Ticket> findByOrderIdOrderById(Long orderId);

  long countByEventId(Long eventId);
}
