package io.macroservice.platform.demo;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.support.ModuleBeans;
import io.macroservice.platform.support.ModuleDataSources;
import io.macroservice.platform.support.ModuleProperties;
import io.macroservice.platform.support.ModuleSettings;
import io.macroservice.platform.support.RpcApiScanner;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.env.Environment;

/** What this process runs, and how it reaches everything else. Never includes a password. */
public class Topology {

    private final ConfigurableListableBeanFactory beanFactory;
    private final ModuleProperties properties;
    private final Environment environment;

    Topology(ConfigurableListableBeanFactory beanFactory, ModuleProperties properties, Environment environment) {
        this.beanFactory = beanFactory;
        this.properties = properties;
        this.environment = environment;
    }

    public Map<String, Object> describe() {
        List<String> packages = AutoConfigurationPackages.has(beanFactory) ? AutoConfigurationPackages.get(beanFactory) : List.of();
        Map<String, Map<String, Object>> modules = new TreeMap<>();
        for (Class<?> api : RpcApiScanner.scan(packages, beanFactory.getBeanClassLoader())) {
            String module = api.getAnnotation(RpcApi.class).value();
            ModuleSettings settings = properties.of(module);
            boolean hasLocal = java.util.Arrays.stream(beanFactory.getBeanNamesForType(api, true, false))
                    .anyMatch(name -> !beanFactory.getBeanDefinition(name).hasAttribute(ModuleBeans.CLIENT_PROXY_ATTRIBUTE));
            String binding = settings.binding() != null ? settings.binding().name().toLowerCase().replace('_', '-') : hasLocal ? "local" : "remote";
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("module", module);
            entry.put("enabled", settings.enabled());
            entry.put("binding", binding);
            entry.put("url", settings.url() != null && !binding.equals("local") ? settings.url() : null);
            entry.put("datasource", settings.enabled() ? datasource(module, settings) : null);
            modules.put(module, entry);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("process", environment.getProperty("spring.application.name", "app"));
        result.put("profiles", List.of(environment.getActiveProfiles()));
        result.put("port", environment.getProperty("local.server.port", environment.getProperty("server.port", "")));
        result.put("modules", new ArrayList<>(modules.values()));
        return result;
    }

    private Map<String, Object> datasource(String module, ModuleSettings settings) {
        ModuleDataSources.Connection connection = ModuleDataSources.resolve(module, settings, environment);
        Map<String, Object> datasource = new LinkedHashMap<>();
        URI uri = URI.create(connection.url().substring("jdbc:".length()));
        datasource.put("host", uri.getHost());
        datasource.put("port", uri.getPort());
        datasource.put("schema", connection.schema());
        return datasource;
    }

    /** Multi-line text for the startup banner. */
    public String banner() {
        StringBuilder text = new StringBuilder("\n  Topology of ").append(describe().get("process")).append(":\n");
        for (Object module : (List<?>) describe().get("modules")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) module;
            text.append("    %-14s %-8s %-15s".formatted(m.get("module"), (Boolean) m.get("enabled") ? "enabled" : "disabled", m.get("binding")));
            if (m.get("datasource") instanceof Map<?, ?> ds) {
                text.append(" db ").append(ds.get("host")).append(':').append(ds.get("port")).append('/').append(ds.get("schema"));
            } else if (m.get("url") != null) {
                text.append(" -> ").append(m.get("url"));
            }
            text.append('\n');
        }
        return text.toString();
    }
}
