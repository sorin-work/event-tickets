package com.sorinwork.tickets.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  Optional<Order> findByAccessToken(String accessToken);

  Optional<Order> findByReference(String reference);

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

  List<Order> findByStatusAndHoldExpiresAtLessThanEqual(OrderStatus status, Instant now);

  @Query(
      """
      select o from Order o
      where o.event.id = :eventId and o.status in :statuses
      order by o.createdAt desc, o.id desc
      """)
  List<Order> findForEvent(
      @Param("eventId") long eventId, @Param("statuses") List<OrderStatus> statuses);

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
