package com.sorinwork.tickets.order;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  interface TierCounts {
    Long getTierId();

    Long getSold();

    Long getHeld();
  }

  @Query(
      """
      select i.tier.id as tierId,
             sum(case when o.status = :paid then i.quantity else 0 end) as sold,
             sum(case when o.status = :pending and o.holdExpiresAt > :now
                      then i.quantity else 0 end) as held
      from OrderItem i join i.order o
      where o.event.id in :eventIds
      group by i.tier.id
      """)
  List<TierCounts> countsForEvents(
      @Param("eventIds") java.util.Collection<Long> eventIds,
      @Param("now") Instant now,
      @Param("paid") OrderStatus paid,
      @Param("pending") OrderStatus pending);

  @Query(
      """
      select i.tier.id as tierId,
             sum(case when o.status = :paid then i.quantity else 0 end) as sold,
             sum(case when o.status = :pending and o.holdExpiresAt > :now
                      then i.quantity else 0 end) as held
      from OrderItem i join i.order o
      where o.event.id = :eventId
      group by i.tier.id
      """)
  List<TierCounts> countsForEvent(
      @Param("eventId") long eventId,
      @Param("now") Instant now,
      @Param("paid") OrderStatus paid,
      @Param("pending") OrderStatus pending);

  interface TierRevenue {
    Long getTierId();

    Long getGross();
  }

  @Query(
      """
      select i.tier.id as tierId, sum(i.quantity * i.unitPriceCents) as gross
      from OrderItem i join i.order o
      where o.event.id = :eventId and o.status = :status
      group by i.tier.id
      """)
  List<TierRevenue> revenueByTier(
      @Param("eventId") long eventId, @Param("status") OrderStatus status);
}
