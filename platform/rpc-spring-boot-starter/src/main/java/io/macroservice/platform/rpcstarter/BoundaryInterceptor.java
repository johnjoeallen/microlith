package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.RpcClientProxy;
import io.micrometer.observation.ObservationRegistry;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;

/**
 * The module boundary for local calls. Wraps every local {@code @RpcApi} implementation, which is what other modules
 * get injected when the binding is {@code local}, so that a local call behaves like a remote one:
 * <ul>
 *   <li>it is observed as {@code macro.rpc.client} with the same tags as a remote call (see {@link RpcObservations});</li>
 *   <li>unless {@code modules.boundary.suspend-transactions=false}, an active transaction of the caller is suspended
 *       for the call (PROPAGATION_NOT_SUPPORTED), because a remote callee could never join it.</li>
 * </ul>
 * It is the outermost wrapper (lowest precedence), so the callee's own {@code @Transactional} starts a new transaction.
 * When the dispatcher invokes the bean for a remote or looped-back caller ({@link BoundaryContext}), the caller's
 * client proxy has already recorded the call, so only the transaction suspension applies.
 */
class BoundaryInterceptor implements BeanPostProcessor, BeanFactoryAware, Ordered {

    private final Environment environment;
    private BeanFactory beanFactory;

    BoundaryInterceptor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof RpcClientProxy) {
            return bean;
        }
        Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bean.getClass());
        RpcApi api = null;
        for (Class<?> type : interfaces) {
            if (type.isAnnotationPresent(RpcApi.class)) {
                api = type.getAnnotation(RpcApi.class);
            }
        }
        if (api == null) {
            return bean;
        }
        return Proxy.newProxyInstance(bean.getClass().getClassLoader(), interfaces, new Handler(bean, api.value()));
    }

    private final class Handler implements InvocationHandler {

        private final Object target;
        private final String module;

        Handler(Object target, String module) {
            this.target = target;
            this.module = module;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!method.getDeclaringClass().isAnnotationPresent(RpcApi.class)) {
                return call(method, args);
            }
            boolean fromDispatcher = BoundaryContext.consumeServerEntry();
            if (fromDispatcher) {
                return suspending(method, args);
            }
            return RpcObservations.observe(registry(), module, method.getName(), "local", () -> suspending(method, args));
        }

        private Object suspending(Method method, Object[] args) throws Exception {
            if (!suspendTransactions() || !TransactionSynchronizationManager.isActualTransactionActive()) {
                return callChecked(method, args);
            }
            Deque<Suspension> suspensions = new ArrayDeque<>();
            // Each manager suspends the transaction it owns; the others just open an empty scope.
            for (PlatformTransactionManager manager : beanFactory.getBeanProvider(PlatformTransactionManager.class).orderedStream().toList()) {
                suspensions.push(new Suspension(manager, manager.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED))));
            }
            try {
                return callChecked(method, args);
            } finally {
                for (Suspension suspension : suspensions) {
                    suspension.manager().commit(suspension.status());
                }
            }
        }

        private Object callChecked(Method method, Object[] args) throws Exception {
            try {
                return call(method, args);
            } catch (Exception | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        }

        private Object call(Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private record Suspension(PlatformTransactionManager manager, TransactionStatus status) {
    }

    private boolean suspendTransactions() {
        return environment.getProperty("modules.boundary.suspend-transactions", Boolean.class, true);
    }

    private ObservationRegistry registry() {
        return beanFactory.getBeanProvider(ObservationRegistry.class).getIfAvailable(() -> ObservationRegistry.NOOP);
    }
}
