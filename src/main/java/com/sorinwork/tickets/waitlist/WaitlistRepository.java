package com.sorinwork.tickets.waitlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

  boolean existsByTierIdAndEmailAndStatusIn(
      Long tierId, String email, List<WaitlistStatus> statuses);

  long countByTierIdAndStatus(Long tierId, WaitlistStatus status);

  @org.springframework.data.jpa.repository.Query(
      """
      select count(w) from WaitlistEntry w
      where w.tier.id = :tierId and w.status = :status
        and (w.createdAt < :createdAt or (w.createdAt = :createdAt and w.id <= :id))
      """)
  long countAheadOrEqual(
      @org.springframework.data.repository.query.Param("tierId") long tierId,
      @org.springframework.data.repository.query.Param("status") WaitlistStatus status,
      @org.springframework.data.repository.query.Param("createdAt") java.time.Instant createdAt,
      @org.springframework.data.repository.query.Param("id") long id);

  Optional<WaitlistEntry> findByOfferOrderId(Long orderId);

  List<WaitlistEntry> findByTierIdAndStatusOrderByCreatedAtAscIdAsc(
      Long tierId, WaitlistStatus status);

  @EntityGraph(attributePaths = "offerOrder")
  Optional<WaitlistEntry> findWithOfferById(Long id);

  @EntityGraph(attributePaths = {"tier", "offerOrder"})
  List<WaitlistEntry> findByEventIdOrderByCreatedAtAscIdAsc(Long eventId);
}
