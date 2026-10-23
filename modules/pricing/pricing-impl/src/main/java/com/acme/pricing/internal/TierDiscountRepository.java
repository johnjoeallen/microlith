package com.acme.pricing.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TierDiscountRepository extends JpaRepository<TierDiscountEntity, String> {
}
