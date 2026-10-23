package com.acme.payments.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthorizationRepository extends JpaRepository<AuthorizationEntity, String> {

    Optional<AuthorizationEntity> findByRequestId(String requestId);
}
