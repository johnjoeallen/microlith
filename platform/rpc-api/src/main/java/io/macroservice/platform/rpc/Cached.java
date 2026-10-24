package io.macroservice.platform.rpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@link RpcApi} method whose result may be reused by callers for a short time: a read of reference data that
 * changes rarely. The caller side keeps the result per argument list (arguments must have value equality) in every
 * binding, local included, so callers behave the same wherever the module runs. Failures and {@code null} results are
 * never cached. Never put this on a method that changes state or reads fast-moving state (stock, reservations).
 * Caching is switched off with {@code rpc.cache.enabled=false}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {

    /** How long a result is reused, in seconds. */
    int ttlSeconds() default 30;

    /** How many distinct argument lists are remembered per method. */
    int maxSize() default 10_000;
}
