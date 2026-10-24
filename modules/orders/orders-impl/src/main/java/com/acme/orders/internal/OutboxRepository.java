package com.acme.orders.internal;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

interface OutboxRepository extends JpaRepository<OutboxEntryEntity, String> {

    /** Due entries; rows locked by another relay instance are skipped, so instances never take the same entry. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from OutboxEntryEntity o where o.status = 'PENDING' and o.nextAttemptAt <= :now order by o.createdAt")
    List<OutboxEntryEntity> claim(@Param("now") Instant now, Pageable page);

    long countByStatus(String status);
}
