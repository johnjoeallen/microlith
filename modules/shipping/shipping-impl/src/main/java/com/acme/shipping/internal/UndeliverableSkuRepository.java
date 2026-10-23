package com.acme.shipping.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UndeliverableSkuRepository extends JpaRepository<UndeliverableSkuEntity, String> {
}
