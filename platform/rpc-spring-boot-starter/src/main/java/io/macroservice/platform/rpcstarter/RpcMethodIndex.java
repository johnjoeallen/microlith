package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.RpcApi;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reflection view of an {@link RpcApi} interface: one method per name, parameters with their names. */
final class RpcMethodIndex {

    private RpcMethodIndex() {
    }

    /** The methods of the interface by name. Rejects overloaded names and missing parameter names. */
    static Map<String, Method> methodsOf(Class<?> api) {
        Map<String, Method> methods = new LinkedHashMap<>();
        Arrays.stream(api.getMethods()).filter(m -> !Modifier.isStatic(m.getModifiers()))
                .sorted(Comparator.comparing(Method::getName)).forEach(method -> {
                    Method previous = methods.put(method.getName(), method);
                    if (previous != null) {
                        throw new IllegalStateException("@RpcApi interface %s has an overloaded method '%s'; JSON-RPC method names must be unique"
                                .formatted(api.getName(), method.getName()));
                    }
                    for (Parameter parameter : method.getParameters()) {
                        if (!parameter.isNamePresent()) {
                            throw new IllegalStateException("Parameter names of %s.%s are not available; compile with -parameters"
                                    .formatted(api.getName(), method.getName()));
                        }
                    }
                });
        return methods;
    }
}
