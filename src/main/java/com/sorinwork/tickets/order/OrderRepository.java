package com.sorinwork.tickets.order;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  Optional<Order> findByAccessToken(String accessToken);

  /** The order with its event, items and tiers loaded, for rendering checkout pages. */
  @Query(
      """
      select distinct o from Order o
      join fetch o.event
      left join fetch o.promoCode
      left join fetch o.items i
      left join fetch i.tier
      where o.accessToken = :token
      """)
  Optional<Order> findWithItemsByAccessToken(@Param("token") String token);

  /**
   * Takes a shared row lock on the order ({@code FOR SHARE} on PostgreSQL). Check-in holds it so a
   * cancellation cannot commit between checking that the order is paid and admitting its ticket.
   */
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("select o from Order o where o.id = :id")
  Optional<Order> lockForShare(@Param("id") long id);

  List<Order> findByStatusAndHoldExpiresAtLessThanEqual(OrderStatus status, Instant now);

  // Lock order (docs/architecture.md): order rows first, in id order, then the event row.

  /** The order with a row lock, for changes a buyer and an admin or a job could make at once. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.accessToken = :token")
  Optional<Order> lockByAccessToken(@Param("token") String token);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.id = :id")
  Optional<Order> lockById(@Param("id") long id);

  /** Locks the event's orders with the given statuses, before the event itself is changed. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select o from Order o where o.event.id = :eventId and o.status in :statuses order by o.id")
  List<Order> lockForEvent(
      @Param("eventId") long eventId, @Param("statuses") List<OrderStatus> statuses);

  /**
   * Holds that ran out, locked; rows another transaction is changing right now (a buyer paying or
   * giving the seats back) are skipped and left for the next run ({@code SKIP LOCKED}).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
  @Query("select o from Order o where o.status = :status and o.holdExpiresAt <= :now order by o.id")
  List<Order> lockRunOutHolds(@Param("status") OrderStatus status, @Param("now") Instant now);

  @Query(
      value =
          """
          select o from Order o
          where o.event.id = :eventId and o.status in :statuses
          order by o.createdAt desc, o.id desc
          """,
      countQuery =
          """
          select count(o) from Order o
          where o.event.id = :eventId and o.status in :statuses
          """)
  Page<Order> findForEvent(
      @Param("eventId") long eventId,
      @Param("statuses") List<OrderStatus> statuses,
      Pageable pageable);

  @Query(
      """
      select distinct o from Order o
      join fetch o.event
      left join fetch o.promoCode
      left join fetch o.items i
      left join fetch i.tier
      where o.id = :id
      """)
  Optional<Order> findWithItemsById(@Param("id") long id);

  interface Totals {
    Long getOrders();

    Long getSubtotal();

    Long getDiscount();

    Long getTotal();
  }

  @Query(
      """
      select count(o) as orders, coalesce(sum(o.subtotalCents), 0) as subtotal,
             coalesce(sum(o.discountCents), 0) as discount, coalesce(sum(o.totalCents), 0) as total
      from Order o where o.event.id = :eventId and o.status = :status
      """)
  Totals totalsForEvent(@Param("eventId") long eventId, @Param("status") OrderStatus status);

  long countByEventIdAndStatus(Long eventId, OrderStatus status);

  /** Paid orders without a reminder for published events starting between two local times. */
  @Query(
      """
      select o from Order o join fetch o.event e
      where o.status = :paid and o.reminderSentAt is null
        and e.status = com.sorinwork.tickets.event.EventStatus.PUBLISHED
        and e.startsAt between :from and :to
      """)
  List<Order> findReminderCandidates(
      @Param("paid") OrderStatus paid,
      @Param("from") java.time.LocalDateTime from,
      @Param("to") java.time.LocalDateTime to);

  @Query(
      """
      select o from Order o join fetch o.event e
      where o.buyerEmail = :email and o.status = :status and e.startsAt > :after
      order by e.startsAt
      """)
  List<Order> findUpcomingForBuyer(
      @Param("email") String email,
      @Param("status") OrderStatus status,
      @Param("after") java.time.LocalDateTime after);

  @Query(
      """
      select o from Order o join fetch o.event
      where o.status = :status
      order by o.paidAt desc, o.id desc
      """)
  List<Order> findRecent(@Param("status") OrderStatus status, Pageable pageable);

  long countByStatusAndPaidAtAfter(OrderStatus status, Instant after);

  interface PaidOrderRow {
    Instant getPaidAt();

    Long getTotal();

    Long getTickets();
  }

  @Query(
      """
      select o.paidAt as paidAt, o.totalCents as total, sum(i.quantity) as tickets
      from Order o join o.items i
      where o.event.id = :eventId and o.status = :status
      group by o.id, o.paidAt, o.totalCents
      order by o.paidAt
      """)
  List<PaidOrderRow> paidOrderRows(
      @Param("eventId") long eventId, @Param("status") OrderStatus status);

  interface PromoUsageRow {
    String getCode();

    Long getOrders();

    Long getDiscount();
  }

  @Query(
      """
      select p.code as code, count(o) as orders, sum(o.discountCents) as discount
      from Order o join o.promoCode p
      where o.event.id = :eventId and o.status = :status
      group by p.code
      order by count(o) desc, p.code
      """)
  List<PromoUsageRow> promoUsage(
      @Param("eventId") long eventId, @Param("status") OrderStatus status);
}
