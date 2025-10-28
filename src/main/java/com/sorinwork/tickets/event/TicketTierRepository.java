package com.sorinwork.tickets.event;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {

  List<TicketTier> findByEventIdOrderByPositionAscIdAsc(Long eventId);

  Optional<TicketTier> findByIdAndEventId(Long id, Long eventId);

  int countByEventId(Long eventId);
}
