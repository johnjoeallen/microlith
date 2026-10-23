package io.macroservice.platform.support;

import io.macroservice.platform.rpc.RpcApi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.core.env.Environment;

/**
 * Checks, for every {@link RpcApi} interface, that the module is either enabled with a datasource and a local
 * implementation, or disabled with a client URL. Collects all problems instead of stopping at the first.
 */
public class ModuleConfigurationValidator {

    private final Environment environment;
    private final ModuleProperties properties;

    public ModuleConfigurationValidator(Environment environment, ModuleProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    public List<String> validate(Collection<Class<?>> rpcApis, Predicate<Class<?>> hasLocalImplementation) {
        List<String> problems = new ArrayList<>();
        Map<String, Class<?>> byModule = new HashMap<>();
        for (Class<?> api : rpcApis) {
            String module = api.getAnnotation(RpcApi.class).value();
            Class<?> previous = byModule.put(module, api);
            if (previous != null) {
                problems.add("Module name '%s' is used by more than one @RpcApi interface: %s and %s"
                        .formatted(module, previous.getName(), api.getName()));
                continue;
            }
            ModuleSettings settings = properties.of(module);
            boolean enabled = environment.getProperty("modules." + module + ".enabled", Boolean.class, false);
            if (enabled) {
                if (!hasLocalImplementation.test(api)) {
                    problems.add("Module '%s' is enabled but no implementation of %s is on the classpath"
                            .formatted(module, api.getSimpleName()));
                } else if (!hasDatasource(settings)) {
                    problems.add("Module '%s' is enabled but has no datasource: set modules.%s.datasource.url or spring.datasource.url"
                            .formatted(module, module));
                }
            } else if (!settings.hasUrl()) {
                problems.add("Module '%s' is disabled but modules.%s.url is not set".formatted(module, module));
            }
        }
        return problems;
    }

    private boolean hasDatasource(ModuleSettings settings) {
        if (settings.datasource().hasUrl()) {
            return true;
        }
        String shared = environment.getProperty("spring.datasource.url");
        return shared != null && !shared.isBlank();
    }
}
