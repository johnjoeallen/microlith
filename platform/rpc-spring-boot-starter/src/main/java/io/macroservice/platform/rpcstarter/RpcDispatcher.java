package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.RpcClientProxy;
import io.macroservice.platform.rpc.RpcError;
import io.macroservice.platform.rpc.RpcErrorCodes;
import io.macroservice.platform.rpc.RpcException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON-RPC 2.0 server core, independent of HTTP. Indexes every local bean that implements an {@link RpcApi}
 * interface (client proxies are excluded) as {@code <module>.<method>} and dispatches by reflection.
 */
public class RpcDispatcher implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(RpcDispatcher.class);

    private record Target(Object bean, Method method, Parameter[] parameters, JavaType[] types) {
    }

    private final JsonMapper mapper;
    private final ListableBeanFactory beans;
    private final List<RpcServerInterceptor> interceptors;
    private volatile Map<String, Target> index;
    private volatile Map<String, Class<?>> apis = Map.of();

    public RpcDispatcher(JsonMapper mapper, ListableBeanFactory beans, List<RpcServerInterceptor> interceptors) {
        this.mapper = mapper;
        this.beans = beans;
        this.interceptors = interceptors;
    }

    /** Builds the index at startup so that an invalid API (for example an overload) fails the application early. */
    @Override
    public void afterSingletonsInstantiated() {
        index();
    }

    /**
     * Handles one JSON-RPC request body for the endpoint of {@code module}.
     *
     * @return the response (an object, or an array for a batch), or null when nothing is to be sent back
     *         (a notification, or a batch of notifications)
     */
    public JsonNode handleEnvelope(String module, String body) {
        JsonNode root;
        try {
            root = body == null || body.isBlank() ? null : mapper.readTree(body);
        } catch (JacksonException e) {
            root = null;
        }
        if (root == null) {
            return error(null, RpcErrorCodes.PARSE_ERROR, "Parse error", null);
        }
        if (root.isArray()) {
            if (root.isEmpty()) {
                return error(null, RpcErrorCodes.INVALID_REQUEST, "Invalid Request", null);
            }
            ArrayNode responses = mapper.createArrayNode();
            for (JsonNode element : root) {
                JsonNode response = handleSingle(module, element);
                if (response != null) {
                    responses.add(response);
                }
            }
            return responses.isEmpty() ? null : responses;
        }
        return handleSingle(module, root);
    }

    private JsonNode handleSingle(String module, JsonNode request) {
        if (!request.isObject()) {
            return error(null, RpcErrorCodes.INVALID_REQUEST, "Invalid Request", null);
        }
        boolean notification = !request.has("id");
        JsonNode id = notification ? null : request.get("id");
        if (id != null && !(id.isString() || id.isNumber() || id.isNull())) {
            return error(null, RpcErrorCodes.INVALID_REQUEST, "Invalid Request: id must be a string, number or null", null);
        }
        JsonNode version = request.get("jsonrpc");
        JsonNode method = request.get("method");
        if (version == null || !version.isString() || !"2.0".equals(version.stringValue())
                || method == null || !method.isString()) {
            return error(id, RpcErrorCodes.INVALID_REQUEST, "Invalid Request", null);
        }
        JsonNode params = request.get("params");
        if (params == null || params.isNull()) {
            params = mapper.createObjectNode();
        } else if (!params.isObject()) {
            return notification ? null : error(id, RpcErrorCodes.INVALID_PARAMS,
                    "Invalid params: parameters must be passed by name (a JSON object)", null);
        }
        String prefix = module + ".";
        String name = method.stringValue();
        try {
            if (!name.startsWith(prefix)) {
                throw new RpcCallException(RpcErrorCodes.METHOD_NOT_FOUND, "Method not found: " + name, null);
            }
            JsonNode result = call(module, name.substring(prefix.length()), params);
            return notification ? null : success(id, result);
        } catch (RpcCallException e) {
            return notification ? null : error(id, e.code(), e.getMessage(), e.data());
        }
    }

    /** Calls one method with by-name parameters; failures are reported as {@link RpcCallException}. */
    public JsonNode call(String module, String method, JsonNode params) {
        Target target = index().get(module + "." + method);
        if (target == null) {
            throw new RpcCallException(RpcErrorCodes.METHOD_NOT_FOUND, "Method not found: " + module + "." + method, null);
        }
        Object[] arguments = bind(target, params);
        try {
            Object result = proceed(0, module, method, () -> invoke(target, arguments));
            return result == null ? mapper.nullNode() : mapper.valueToTree(result);
        } catch (RpcCallException e) {
            throw e;
        } catch (Exception e) {
            throw translate(module, method, e);
        }
    }

    private Object proceed(int position, String module, String method, Callable<Object> invocation) throws Exception {
        if (position == interceptors.size()) {
            return invocation.call();
        }
        return interceptors.get(position).intercept(module, method, () -> proceed(position + 1, module, method, invocation));
    }

    private Object[] bind(Target target, JsonNode params) {
        Object[] arguments = new Object[target.parameters.length];
        for (int i = 0; i < arguments.length; i++) {
            String name = target.parameters[i].getName();
            JsonNode value = params.get(name);
            if (value == null || value.isNull()) {
                if (target.parameters[i].getType().isPrimitive()) {
                    throw new RpcCallException(RpcErrorCodes.INVALID_PARAMS, "Invalid params: missing parameter '" + name + "'", null);
                }
                continue;
            }
            try {
                arguments[i] = mapper.treeToValue(value, target.types[i]);
            } catch (JacksonException | IllegalArgumentException e) {
                throw new RpcCallException(RpcErrorCodes.INVALID_PARAMS, "Invalid params: parameter '" + name + "' is not valid", null);
            }
        }
        return arguments;
    }

    private Object invoke(Target target, Object[] arguments) throws Exception {
        try {
            return target.method.invoke(target.bean, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw (Error) cause;
        }
    }

    private RpcCallException translate(String module, String method, Exception failure) {
        RpcError business = AnnotationUtils.findAnnotation(failure.getClass(), RpcError.class);
        if (business != null) {
            ObjectNode data = mapper.createObjectNode();
            data.put("exception", failure.getClass().getName());
            data.put("message", failure.getMessage());
            return new RpcCallException(business.code(), failure.getMessage(), data);
        }
        if (failure instanceof RpcException rpc) {
            return new RpcCallException(rpc.code(), rpc.getMessage(), null);
        }
        log.error("Unexpected failure in {}.{}", module, method, failure);
        return new RpcCallException(RpcErrorCodes.INTERNAL_ERROR, "Internal error", null);
    }

    private JsonNode success(JsonNode id, JsonNode result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("result", result);
        response.set("id", id);
        return response;
    }

    private JsonNode error(JsonNode id, int code, String message, JsonNode data) {
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.set("data", data);
        }
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("error", error);
        response.set("id", id == null ? mapper.nullNode() : id);
        return response;
    }

    /** The {@link RpcApi} interfaces served by a local implementation in this process, by module name. */
    public Map<String, Class<?>> localApis() {
        index();
        return apis;
    }

    private Map<String, Target> index() {
        Map<String, Target> current = index;
        if (current == null) {
            synchronized (this) {
                if (index == null) {
                    index = buildIndex();
                }
                current = index;
            }
        }
        return current;
    }

    private Map<String, Target> buildIndex() {
        Map<String, Target> targets = new HashMap<>();
        Map<String, Object> moduleBeans = new HashMap<>();
        Map<String, Class<?>> served = new java.util.TreeMap<>();
        for (Object bean : beans.getBeansWithAnnotation(RpcApi.class).values()) {
            if (bean instanceof RpcClientProxy) {
                continue;
            }
            for (Class<?> type : ClassUtils.getAllInterfacesForClassAsSet(bean.getClass())) {
                RpcApi api = type.getAnnotation(RpcApi.class);
                if (api == null) {
                    continue;
                }
                served.put(api.value(), type);
                Object previous = moduleBeans.put(api.value(), bean);
                if (previous != null && previous != bean) {
                    throw new IllegalStateException("More than one local implementation of module '%s': %s and %s"
                            .formatted(api.value(), previous.getClass().getName(), bean.getClass().getName()));
                }
                RpcMethodIndex.methodsOf(type).forEach((name, method) -> {
                    JavaType[] types = new JavaType[method.getParameterCount()];
                    for (int i = 0; i < types.length; i++) {
                        types[i] = mapper.constructType(method.getGenericParameterTypes()[i]);
                    }
                    targets.put(api.value() + "." + name, new Target(bean, method, method.getParameters(), types));
                });
            }
        }
        apis = java.util.Collections.unmodifiableMap(served);
        return Map.copyOf(targets);
    }
}
