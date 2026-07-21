package com.sorinwork.tickets.event;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

  Optional<Event> findBySlug(String slug);

  List<Event> findTop20ByStatusAndStartsAtBeforeOrderByStartsAtDesc(
      EventStatus status, LocalDateTime before);

  @Query("select count(o) from Order o where o.event.id = :id")
  long countOrders(@Param("id") long id);

  boolean existsBySlug(String slug);

  List<Event> findAllByOrderByStartsAtDesc();

  List<Event> findByStatusInAndStartsAtAfterOrderByStartsAt(
      List<EventStatus> statuses, LocalDateTime after);

  /** Loads the event with a row lock so concurrent checkouts cannot oversell it. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Event e where e.id = :id")
  Optional<Event> lockById(@Param("id") long id);
}
