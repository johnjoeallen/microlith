package com.acme.orders.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<OrderEntity, String> {

    Optional<OrderEntity> findByRequestId(String requestId);
}
