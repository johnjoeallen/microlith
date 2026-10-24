package io.macroservice.platform.rpcstarter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.macroservice.platform.rpc.CacheLayer;
import io.macroservice.platform.rpc.CacheSignals;
import io.macroservice.platform.rpc.Cached;
import io.macroservice.platform.rpc.RpcApi;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * Puts a Caffeine cache in front of the {@link Cached} methods of every bean that implements an {@code @RpcApi}
 * interface: local implementations and client proxies alike, so a call costs the same wherever the module runs. The bean
 * is wrapped, never replaced (all its interfaces are kept, including {@code RpcClientProxy}). It runs after the AOP
 * proxy creators, so transactions stay inside, and before the demo call tracer, which sees hits as binding {@code cache}.
 */
class CachingBeanPostProcessor implements BeanPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bean.getClass());
        Map<Method, Cache<List<Object>, Object>> caches = new HashMap<>();
        for (Class<?> type : interfaces) {
            if (type.isAnnotationPresent(RpcApi.class)) {
                for (Method method : type.getMethods()) {
                    Cached cached = method.getAnnotation(Cached.class);
                    if (cached != null) {
                        caches.put(method, Caffeine.newBuilder().maximumSize(cached.maxSize())
                                .expireAfterWrite(Duration.ofSeconds(cached.ttlSeconds())).build());
                    }
                }
            }
        }
        if (caches.isEmpty()) {
            return bean;
        }
        Class<?>[] withMarker = Arrays.copyOf(interfaces, interfaces.length + 1);
        withMarker[interfaces.length] = CacheLayer.class;
        return Proxy.newProxyInstance(bean.getClass().getClassLoader(), withMarker, new Handler(bean, caches));
    }

    private record Handler(Object target, Map<Method, Cache<List<Object>, Object>> caches) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            boolean bypass = CacheSignals.consumeBypass();
            Cache<List<Object>, Object> cache = bypass ? null : caches.get(method);
            if (cache == null) {
                return call(method, args);
            }
            List<Object> key = args == null ? List.of() : Arrays.asList(args.clone());
            Object cached = cache.getIfPresent(key);
            if (cached != null) {
                CacheSignals.markHit();
                return cached;
            }
            Object value = call(method, args);
            CacheSignals.consumeHit();                 // a hit by a call nested inside this one is not ours
            if (value != null) {
                cache.put(key, value);
            }
            return value;
        }

        private Object call(Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
