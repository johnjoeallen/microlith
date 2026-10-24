package com.acme.orders.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Spring Modulith's {@code @ApplicationModuleListener} is {@code @Async} + {@code @Transactional(REQUIRES_NEW)} +
 * {@code @TransactionalEventListener}. The {@code @Transactional} part has no place here: it cannot name a transaction manager (there is one
 * per module), and these listeners only call other modules, so holding an orders connection open for the call would waste the pool.
 * This is the same listener without it.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Async
@TransactionalEventListener
@interface OrdersEventListener {
}
