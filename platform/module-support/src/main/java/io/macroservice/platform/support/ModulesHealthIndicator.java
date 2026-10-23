package io.macroservice.platform.support;

import java.util.Collection;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/** Readiness of the enabled modules: DOWN until every enabled module's schema is migrated. */
public class ModulesHealthIndicator implements HealthIndicator {

    private final Collection<ModuleMigration> migrations;

    public ModulesHealthIndicator(Collection<ModuleMigration> migrations) {
        this.migrations = migrations;
    }

    @Override
    public Health health() {
        Health.Builder health = Health.up();
        for (ModuleMigration migration : migrations) {
            if (migration.isMigrated()) {
                health.withDetail(migration.module(), "migrated");
            } else {
                health.down().withDetail(migration.module(), "not ready: " + migration.lastError());
            }
        }
        return health.build();
    }
}
