package com.sorinwork.tickets.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

  Optional<Event> findBySlug(String slug);

  boolean existsBySlug(String slug);

  List<Event> findAllByOrderByStartsAtDesc();

  List<Event> findByStatusInAndStartsAtAfterOrderByStartsAt(
      List<EventStatus> statuses, LocalDateTime after);
}
