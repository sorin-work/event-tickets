package com.sorinwork.tickets.ticket;

import com.sorinwork.tickets.order.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

  Optional<Ticket> findByCode(String code);

  @EntityGraph(attributePaths = "tier")
  List<Ticket> findByOrderIdOrderById(Long orderId);

  long countByEventId(Long eventId);

  @Query(
      value =
          """
          select t from Ticket t join fetch t.tier join fetch t.order o
          where t.event.id = :eventId and o.status = :status
          order by lower(t.attendeeName), t.id
          """,
      countQuery =
          """
          select count(t) from Ticket t join t.order o
          where t.event.id = :eventId and o.status = :status
          """)
  Page<Ticket> findAttendees(
      @Param("eventId") long eventId, @Param("status") OrderStatus status, Pageable pageable);
}
