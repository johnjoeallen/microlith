package io.macroservice.platform.rpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a business exception defined in an {@code -api} artifact so that it can be sent as a JSON-RPC error and
 * rehydrated on the client.
 *
 * <p>The exception must be unchecked and have a public constructor taking a single {@code String} (the message).
 * Codes must be unique across all modules and outside the reserved JSON-RPC range ({@code -32768..-32000}).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcError {

    int code();
}
