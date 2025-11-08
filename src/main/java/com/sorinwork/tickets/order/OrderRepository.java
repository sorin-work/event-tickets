package com.sorinwork.tickets.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

  Optional<Order> findByAccessToken(String accessToken);

  Optional<Order> findByReference(String reference);
}
