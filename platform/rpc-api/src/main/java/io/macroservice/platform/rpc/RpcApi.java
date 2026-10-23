package io.macroservice.platform.rpc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as the public API of a module. This interface is the only way another module may interact with
 * the module: a direct call in-process, a JSON-RPC 2.0 request when the module runs in another process.
 *
 * <p>Rules: no overloaded method names, immutable record parameters and results, unchecked exceptions only, and no
 * types from outside the {@code -api} artifact.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcApi {

    /** The module name, for example {@code inventory}. Also the first part of every JSON-RPC method name. */
    String value();
}
