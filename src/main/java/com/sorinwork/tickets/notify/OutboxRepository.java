package com.sorinwork.tickets.notify;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

  List<OutboxMessage> findByOrderByCreatedAtDescIdDesc(Pageable pageable);

  List<OutboxMessage> findByRecipientOrderByIdAsc(String recipient);
}
