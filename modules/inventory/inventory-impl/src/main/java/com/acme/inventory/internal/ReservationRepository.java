package com.acme.inventory.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationRepository extends JpaRepository<ReservationEntity, String> {

    Optional<ReservationEntity> findByRequestId(String requestId);
}
