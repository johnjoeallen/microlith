package io.macroservice.platform.support;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

/**
 * Compares the pool sizes of the enabled modules with {@code platform.db.connection-budget} (the connections this
 * deployment may use per instance). Warns, or fails startup when {@code platform.db.enforce-budget=true}.
 */
public class ConnectionBudgetCheck implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ConnectionBudgetCheck.class);

    private final Environment environment;
    private final ModuleProperties properties;

    public ConnectionBudgetCheck(Environment environment, ModuleProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        Integer budget = environment.getProperty("platform.db.connection-budget", Integer.class);
        if (budget == null) {
            return;
        }
        int total = 0;
        StringBuilder detail = new StringBuilder();
        for (Map.Entry<String, ModuleSettings> module : properties.entrySet()) {
            if (module.getValue().enabled()) {
                int size = module.getValue().datasource().maxPoolSize();
                total += size;
                detail.append(detail.isEmpty() ? "" : ", ").append(module.getKey()).append('=').append(size);
            }
        }
        if (total > budget) {
            String message = "Connection budget exceeded: enabled modules have max-pool-size %d in total (%s) but platform.db.connection-budget is %d"
                    .formatted(total, detail, budget);
            if (environment.getProperty("platform.db.enforce-budget", Boolean.class, false)) {
                throw new ModuleConfigurationException(message);
            }
            log.warn(message);
        }
    }
}
