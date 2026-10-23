package io.macroservice.platform.demo;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.RpcClientProxy;
import io.macroservice.platform.support.ModuleProperties;
import io.macroservice.platform.support.ModuleSettings.Binding;
import java.lang.reflect.Proxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;

/**
 * Puts a {@link CallTracer} around every bean that implements an {@code @RpcApi} interface, so that the calls other
 * modules inject are traced whether they are direct, looped back or remote. Beans are wrapped, never replaced:
 * the dispatcher still sees the same interfaces (and {@link RpcClientProxy}).
 */
class TracingBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private final TraceStore store;
    private BeanFactory beanFactory;

    TracingBeanPostProcessor(TraceStore store) {
        this.store = store;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
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
        String module = api.value();
        return Proxy.newProxyInstance(bean.getClass().getClassLoader(), interfaces, new CallTracer(bean, module, bindingOf(bean, module), store));
    }

    private String bindingOf(Object bean, String module) {
        if (!(bean instanceof RpcClientProxy)) {
            return "local";
        }
        Binding binding = beanFactory.getBean(ModuleProperties.class).of(module).binding();
        return binding == null ? "remote" : binding.name().toLowerCase().replace('_', '-');
    }
}
