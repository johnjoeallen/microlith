package com.acme.shipping.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShipmentRepository extends JpaRepository<ShipmentEntity, String> {

    Optional<ShipmentEntity> findByRequestId(String requestId);
}
