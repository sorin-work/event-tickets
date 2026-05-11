package com.sorinwork.tickets.waitlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

  boolean existsByTierIdAndEmailAndStatusIn(
      Long tierId, String email, List<WaitlistStatus> statuses);

  long countByTierIdAndStatus(Long tierId, WaitlistStatus status);

  Optional<WaitlistEntry> findByOfferOrderId(Long orderId);

  List<WaitlistEntry> findByTierIdAndStatusOrderByCreatedAtAscIdAsc(
      Long tierId, WaitlistStatus status);

  @EntityGraph(attributePaths = "offerOrder")
  Optional<WaitlistEntry> findWithOfferById(Long id);

  @EntityGraph(attributePaths = {"tier", "offerOrder"})
  List<WaitlistEntry> findByEventIdOrderByCreatedAtAscIdAsc(Long eventId);
}
