package io.macroservice.platform.rpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@link RpcApi} method as safe to repeat (a pure read, or a mutation keyed by a client-supplied request id).
 * Only such methods are retried by a client proxy, and only when retries are configured.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
}
