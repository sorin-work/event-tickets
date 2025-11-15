package com.sorinwork.tickets.order;

import java.util.Optional;
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
      left join fetch o.items i
      left join fetch i.tier
      where o.accessToken = :token
      """)
  Optional<Order> findWithItemsByAccessToken(@Param("token") String token);
}
