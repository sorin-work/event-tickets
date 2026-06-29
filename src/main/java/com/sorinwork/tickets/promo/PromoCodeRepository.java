package com.sorinwork.tickets.promo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

  @EntityGraph(attributePaths = "tier")
  List<PromoCode> findByEventIdOrderByCode(Long eventId);

  Optional<PromoCode> findByEventIdAndCode(Long eventId, String code);

  Optional<PromoCode> findByIdAndEventId(Long id, Long eventId);

  boolean existsByEventIdAndCode(Long eventId, String code);
}
