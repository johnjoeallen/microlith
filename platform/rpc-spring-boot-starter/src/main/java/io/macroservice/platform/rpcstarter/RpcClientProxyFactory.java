package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.Idempotent;
import io.macroservice.platform.rpc.RpcClientProxy;
import io.macroservice.platform.rpc.RpcError;
import io.macroservice.platform.rpc.RpcErrorCodes;
import io.macroservice.platform.rpc.RpcProtocolException;
import io.macroservice.platform.rpc.RpcRemoteException;
import io.macroservice.platform.rpc.RpcUnavailableException;
import io.macroservice.platform.support.ModuleSettings;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Creates {@code java.lang.reflect.Proxy} clients for {@code @RpcApi} interfaces. */
public class RpcClientProxyFactory {

    private final JsonMapper mapper;
    private final List<RpcHeaderPropagator> propagators;

    public RpcClientProxyFactory(JsonMapper mapper, List<RpcHeaderPropagator> propagators) {
        this.mapper = mapper;
        this.propagators = propagators;
    }

    public <T> T create(Class<T> api, String module, RpcTransport transport, ModuleSettings.Client client) {
        RpcMethodIndex.methodsOf(api);
        Object proxy = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] {api, RpcClientProxy.class},
                new Handler(api, module, transport, client));
        return api.cast(proxy);
    }

    private final class Handler implements InvocationHandler {

        private final Class<?> api;
        private final String module;
        private final RpcTransport transport;
        private final ModuleSettings.Client client;
        private final AtomicLong ids = new AtomicLong();

        Handler(Class<?> api, String module, RpcTransport transport, ModuleSettings.Client client) {
            this.api = api;
            this.module = module;
            this.transport = transport;
            this.client = client;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "RpcClient[" + module + " via " + transport.getClass().getSimpleName() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            long id = ids.incrementAndGet();
            String request = mapper.writeValueAsString(envelope(method, args, id));
            int attempts = 1 + (method.isAnnotationPresent(Idempotent.class) ? Math.max(0, client.retries()) : 0);
            for (int attempt = 1; ; attempt++) {
                try {
                    return interpret(method, id, transport.send(module, request, headers()));
                } catch (RpcUnavailableException e) {
                    if (attempt >= attempts) {
                        throw e;
                    }
                    Thread.sleep(client.retryBackoff().toMillis());
                }
            }
        }

        private ObjectNode envelope(Method method, Object[] args, long id) {
            ObjectNode params = mapper.createObjectNode();
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                params.set(parameters[i].getName(), mapper.valueToTree(args[i]));
            }
            ObjectNode request = mapper.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("method", module + "." + method.getName());
            request.set("params", params);
            request.put("id", id);
            return request;
        }

        private Map<String, String> headers() {
            Map<String, String> headers = new LinkedHashMap<>();
            propagators.forEach(p -> p.contribute(headers));
            return headers;
        }

        private Object interpret(Method method, long id, String responseJson) {
            if (responseJson == null) {
                throw new RpcProtocolException("Module '%s' sent no response to %s".formatted(module, method.getName()));
            }
            JsonNode response;
            try {
                response = mapper.readTree(responseJson);
            } catch (JacksonException e) {
                throw new RpcProtocolException("Module '%s' sent a malformed response".formatted(module), e);
            }
            if (!response.isObject() || !"2.0".equals(text(response.get("jsonrpc")))) {
                throw new RpcProtocolException("Module '%s' sent a response that is not JSON-RPC 2.0".formatted(module));
            }
            JsonNode error = response.get("error");
            if (error != null && !error.isNull()) {
                throw toException(error);
            }
            JsonNode responseId = response.get("id");
            if (responseId == null || !responseId.isNumber() || responseId.asLong() != id) {
                throw new RpcProtocolException("Module '%s' answered with an unexpected response id".formatted(module));
            }
            JsonNode result = response.get("result");
            if (method.getReturnType() == void.class || result == null || result.isNull()) {
                return null;
            }
            try {
                return mapper.treeToValue(result, mapper.constructType(method.getGenericReturnType()));
            } catch (JacksonException e) {
                throw new RpcProtocolException("Module '%s' returned a result that does not match %s".formatted(module, method.getName()), e);
            }
        }

        private RuntimeException toException(JsonNode error) {
            int code = error.path("code").asInt(RpcErrorCodes.INTERNAL_ERROR);
            String message = text(error.get("message"));
            message = message == null ? "Remote error " + code : message;
            JsonNode data = error.get("data");
            String className = data == null ? null : text(data.get("exception"));
            if (className != null) {
                RuntimeException business = rehydrate(className, code, message);
                if (business != null) {
                    return business;
                }
            }
            return switch (code) {
                case RpcErrorCodes.PARSE_ERROR, RpcErrorCodes.INVALID_REQUEST, RpcErrorCodes.METHOD_NOT_FOUND,
                        RpcErrorCodes.INVALID_PARAMS -> new RpcProtocolException(code, message);
                case RpcErrorCodes.OVERLOADED, RpcErrorCodes.UNAVAILABLE -> new RpcUnavailableException(code, message);
                default -> new RpcRemoteException(code, message);
            };
        }

        /** Recreates the exact business exception, but only for classes marked {@code @RpcError} with the same code. */
        private RuntimeException rehydrate(String className, int code, String message) {
            try {
                Class<?> type = ClassUtils.forName(className, api.getClassLoader());
                RpcError marker = AnnotationUtils.findAnnotation(type, RpcError.class);
                if (marker == null || marker.code() != code || !RuntimeException.class.isAssignableFrom(type)) {
                    return null;
                }
                return (RuntimeException) type.getConstructor(String.class).newInstance(message);
            } catch (ReflectiveOperationException | LinkageError e) {
                return null;
            }
        }

        private String text(JsonNode node) {
            return node != null && node.isString() ? node.stringValue() : null;
        }
    }
}
