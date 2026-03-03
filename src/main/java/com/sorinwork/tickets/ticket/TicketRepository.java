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

  Optional<Ticket> findByIdAndOrderAccessToken(Long id, String accessToken);

  @EntityGraph(attributePaths = "tier")
  List<Ticket> findByOrderIdOrderById(Long orderId);

  long countByEventId(Long eventId);

  @Query(
      value =
          """
          select t from Ticket t join fetch t.tier join fetch t.order o
          where t.event.id = :eventId and o.status = :status
            and (:query = '' or lower(t.attendeeName) like :pattern
                 or lower(t.attendeeEmail) like :pattern
                 or t.code = upper(:query) or o.reference = upper(:query))
          order by lower(t.attendeeName), t.id
          """,
      countQuery =
          """
          select count(t) from Ticket t join t.order o
          where t.event.id = :eventId and o.status = :status
            and (:query = '' or lower(t.attendeeName) like :pattern
                 or lower(t.attendeeEmail) like :pattern
                 or t.code = upper(:query) or o.reference = upper(:query))
          """)
  Page<Ticket> findAttendees(
      @Param("eventId") long eventId,
      @Param("status") OrderStatus status,
      @Param("query") String query,
      @Param("pattern") String pattern,
      Pageable pageable);

  List<Ticket> findByEventIdAndOrderStatusOrderById(Long eventId, OrderStatus status);

  @Query(
      """
      select t from Ticket t join fetch t.tier join fetch t.order join fetch t.event
      where t.code = :code
      """)
  Optional<Ticket> findWithDetailsByCode(@Param("code") String code);

  long countByEventIdAndCheckedInAtIsNotNullAndOrderStatus(Long eventId, OrderStatus status);

  long countByEventIdAndOrderStatus(Long eventId, OrderStatus status);

  interface EventCount {
    Long getEventId();

    Long getTickets();
  }

  @Query(
      """
      select t.event.id as eventId, count(t) as tickets
      from Ticket t join t.order o
      where o.status = :status
      group by t.event.id
      """)
  List<EventCount> countPerEvent(@Param("status") OrderStatus status);
}
