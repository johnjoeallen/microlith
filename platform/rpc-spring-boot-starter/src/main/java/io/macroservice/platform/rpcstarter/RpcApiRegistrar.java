package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.support.ModuleBeans;
import io.macroservice.platform.support.ModuleConfigurationException;
import io.macroservice.platform.support.ModuleProperties;
import io.macroservice.platform.support.ModuleSettings;
import io.macroservice.platform.support.ModuleSettings.Binding;
import io.macroservice.platform.support.RpcApiScanner;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs after component scanning and decides, for every {@code @RpcApi} interface, which bean serves it: the local
 * implementation ({@code local}), or a generated client proxy ({@code loopback-codec}, {@code loopback-http},
 * {@code remote}). Bean lookups use {@code allowEagerInit=false}, so nothing is instantiated here.
 */
class RpcApiRegistrar implements BeanDefinitionRegistryPostProcessor {

    private final Environment environment;

    RpcApiRegistrar(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
        List<String> packages = AutoConfigurationPackages.has((BeanFactory) beanFactory)
                ? AutoConfigurationPackages.get(beanFactory) : List.of();
        ModuleProperties properties = Binder.get(environment).bind("modules", Bindable.of(ModuleProperties.class))
                .orElseGet(ModuleProperties::new);
        for (Class<?> api : RpcApiScanner.scan(packages, beanFactory.getBeanClassLoader())) {
            register(registry, beanFactory, api, properties);
        }
    }

    private void register(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory, Class<?> api,
            ModuleProperties properties) {
        String module = api.getAnnotation(RpcApi.class).value();
        RpcMethodIndex.methodsOf(api);
        ModuleSettings settings = properties.of(module);
        List<String> localImplementations = localImplementations(beanFactory, api);
        boolean hasLocal = !localImplementations.isEmpty();
        Binding binding = settings.binding() != null ? settings.binding() : hasLocal ? Binding.LOCAL : Binding.REMOTE;

        switch (binding) {
            case LOCAL -> {
                if (!hasLocal) {
                    throw new ModuleConfigurationException(
                            "Module '%s' has binding local but no local implementation is available".formatted(module));
                }
                return;
            }
            case LOOPBACK_CODEC, LOOPBACK_HTTP -> {
                if (environment.acceptsProfiles(Profiles.of("prod", "production"))) {
                    throw new ModuleConfigurationException(
                            "Module '%s' uses binding %s, which is not allowed in a production profile".formatted(module, name(binding)));
                }
                if (!hasLocal) {
                    throw new ModuleConfigurationException(
                            "Module '%s' uses binding %s, which needs a local implementation".formatted(module, name(binding)));
                }
                if (binding == Binding.LOOPBACK_HTTP && !environment.getProperty("rpc.server.enabled", Boolean.class, true)) {
                    throw new ModuleConfigurationException(
                            "Module '%s' uses binding loopback-http, which needs rpc.server.enabled=true".formatted(module));
                }
            }
            case REMOTE -> {
                if (!settings.hasUrl()) {
                    throw new ModuleConfigurationException(settings.enabled()
                            ? "Module '%s' uses binding remote but modules.%s.url is not set".formatted(module, module)
                            : "Module '%s' is disabled but modules.%s.url is not set".formatted(module, module));
                }
            }
        }

        // The proxy must win injection over a local implementation that is present but bypassed.
        for (String implementation : localImplementations) {
            registry.getBeanDefinition(implementation).setAutowireCandidate(false);
        }
        RootBeanDefinition proxy = new RootBeanDefinition(api);
        proxy.setInstanceSupplier(() -> createProxy(beanFactory, api, module, settings, binding));
        proxy.setPrimary(true);
        proxy.setAttribute(ModuleBeans.CLIENT_PROXY_ATTRIBUTE, Boolean.TRUE);
        registry.registerBeanDefinition(module + "RpcClient", proxy);
    }

    private Object createProxy(BeanFactory beanFactory, Class<?> api, String module, ModuleSettings settings, Binding binding) {
        JsonMapper mapper = beanFactory.getBeanProvider(JsonMapper.class).getIfAvailable(() -> JsonMapper.builder().build());
        RpcTransport transport = switch (binding) {
            case LOOPBACK_CODEC -> new CodecRpcTransport(() -> beanFactory.getBean(RpcDispatcher.class), mapper);
            case LOOPBACK_HTTP, REMOTE -> new HttpRpcTransport(() -> urlOf(module, settings, binding), settings.client());
            case LOCAL -> throw new IllegalStateException("local needs no proxy");
        };
        return beanFactory.getBean(RpcClientProxyFactory.class).create(api, module, transport, settings.client(), name(binding));
    }

    private String urlOf(String module, ModuleSettings settings, Binding binding) {
        if (settings.hasUrl()) {
            return settings.url();
        }
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "9100"));
        return "http://127.0.0.1:" + port + "/rpc/" + module;
    }

    private static List<String> localImplementations(ConfigurableListableBeanFactory beanFactory, Class<?> api) {
        return java.util.Arrays.stream(beanFactory.getBeanNamesForType(api, true, false))
                .filter(name -> !beanFactory.getBeanDefinition(name).hasAttribute(ModuleBeans.CLIENT_PROXY_ATTRIBUTE))
                .toList();
    }

    private static String name(Binding binding) {
        return binding.name().toLowerCase().replace('_', '-');
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
