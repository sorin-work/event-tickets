package com.sorinwork.tickets.ticket;

import com.sorinwork.tickets.order.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

  Optional<Ticket> findByCode(String code);

  @EntityGraph(attributePaths = "tier")
  List<Ticket> findTop8ByEventIdAndCheckedInAtIsNotNullOrderByCheckedInAtDesc(Long eventId);

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
            and (:arrival = 'all'
                 or (:arrival = 'in' and t.checkedInAt is not null)
                 or (:arrival = 'out' and t.checkedInAt is null))
          order by lower(t.attendeeName), t.id
          """,
      countQuery =
          """
          select count(t) from Ticket t join t.order o
          where t.event.id = :eventId and o.status = :status
            and (:query = '' or lower(t.attendeeName) like :pattern
                 or lower(t.attendeeEmail) like :pattern
                 or t.code = upper(:query) or o.reference = upper(:query))
            and (:arrival = 'all'
                 or (:arrival = 'in' and t.checkedInAt is not null)
                 or (:arrival = 'out' and t.checkedInAt is null))
          """)
  Page<Ticket> findAttendees(
      @Param("eventId") long eventId,
      @Param("status") OrderStatus status,
      @Param("query") String query,
      @Param("pattern") String pattern,
      @Param("arrival") String arrival,
      Pageable pageable);

  /** Valid tickets with tier and order, in one query, for exports. */
  @Query(
      """
      select t from Ticket t join fetch t.tier join fetch t.order o
      where t.event.id = :eventId and o.status = :status
      order by t.id
      """)
  List<Ticket> findForExport(@Param("eventId") long eventId, @Param("status") OrderStatus status);

  @Query(
      """
      select t from Ticket t join fetch t.tier join fetch t.order join fetch t.event
      where t.code = :code
      """)
  Optional<Ticket> findWithDetailsByCode(@Param("code") String code);

  /**
   * Checks the ticket in unless it already is or its order is no longer paid, in one statement so
   * two scanners cannot both admit it.
   *
   * @return 1 when this call checked the ticket in, 0 otherwise
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Ticket t set t.checkedInAt = :at, t.checkedInBy = :by
      where t.id = :id and t.checkedInAt is null
        and t.order.id in (select o.id from Order o where o.status = :paid)
      """)
  int markCheckedIn(
      @Param("id") long id,
      @Param("at") Instant at,
      @Param("by") String by,
      @Param("paid") OrderStatus paid);

  /**
   * Reverts a check-in, if there is one.
   *
   * @return 1 when a check-in was undone, 0 when the ticket was not checked in
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update Ticket t set t.checkedInAt = null, t.checkedInBy = null
      where t.id = :id and t.checkedInAt is not null
      """)
  int clearCheckIn(@Param("id") long id);

  long countByEventIdAndCheckedInAtIsNotNullAndOrderStatus(Long eventId, OrderStatus status);

  long countByEventIdAndOrderStatus(Long eventId, OrderStatus status);

  @Query(
      """
      select t.checkedInAt from Ticket t
      where t.event.id = :eventId and t.checkedInAt is not null
      order by t.checkedInAt
      """)
  List<java.time.Instant> checkInTimes(@Param("eventId") long eventId);

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
