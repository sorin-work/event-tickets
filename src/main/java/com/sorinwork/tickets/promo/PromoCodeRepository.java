package com.sorinwork.tickets.promo;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

  @EntityGraph(attributePaths = "tier")
  List<PromoCode> findByEventIdOrderByCode(Long eventId);

  Optional<PromoCode> findByEventIdAndCode(Long eventId, String code);

  /**
   * Finds the code and locks its row in the same query, so what is read is current. Applying a code
   * is the only thing that adds a use, so taking this lock before counting uses means two buyers
   * cannot both take its last use. Admin changes to the code take the same lock.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PromoCode p where p.event.id = :eventId and p.code = :code")
  Optional<PromoCode> lockByEventIdAndCode(
      @Param("eventId") long eventId, @Param("code") String code);

  /** Same lock as {@link #lockByEventIdAndCode}, by id, for the admin area. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PromoCode p where p.id = :id and p.event.id = :eventId")
  Optional<PromoCode> lockByIdAndEventId(@Param("id") long id, @Param("eventId") long eventId);

  Optional<PromoCode> findByIdAndEventId(Long id, Long eventId);

  boolean existsByEventIdAndCode(Long eventId, String code);

  /** Times a code is in use: paid orders plus checkouts that still hold their seats. */
  @Query(
      """
      select count(o) from Order o
      where o.promoCode.id = :promoId
        and (o.status = com.sorinwork.tickets.order.OrderStatus.PAID
             or (o.status = com.sorinwork.tickets.order.OrderStatus.PENDING
                 and o.holdExpiresAt > :now))
      """)
  long countRedemptions(@Param("promoId") long promoId, @Param("now") Instant now);

  interface Redemptions {
    Long getPromoId();

    Long getUses();
  }

  @Query(
      """
      select o.promoCode.id as promoId, count(o) as uses from Order o
      where o.event.id = :eventId
        and (o.status = com.sorinwork.tickets.order.OrderStatus.PAID
             or (o.status = com.sorinwork.tickets.order.OrderStatus.PENDING
                 and o.holdExpiresAt > :now))
        and o.promoCode is not null
      group by o.promoCode.id
      """)
  List<Redemptions> countRedemptionsForEvent(
      @Param("eventId") long eventId, @Param("now") Instant now);
}
