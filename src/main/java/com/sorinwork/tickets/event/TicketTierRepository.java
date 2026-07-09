package com.sorinwork.tickets.event;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {

  List<TicketTier> findByEventIdOrderByPositionAscIdAsc(Long eventId);

  Optional<TicketTier> findByIdAndEventId(Long id, Long eventId);

  int countByEventId(Long eventId);

  @org.springframework.data.jpa.repository.Query(
      "select count(i) from OrderItem i where i.tier.id = :tierId")
  long countOrderItems(@org.springframework.data.repository.query.Param("tierId") long tierId);
}
