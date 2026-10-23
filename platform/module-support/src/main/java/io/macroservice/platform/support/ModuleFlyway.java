package io.macroservice.platform.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Builds the Flyway of one module: its own locations, schema and history table, and its own migration role. */
public final class ModuleFlyway {

    private ModuleFlyway() {
    }

    /**
     * Migrations live in {@code classpath:db/migration/<module>}; the {@code demo} profile adds the demo-only seed data
     * in {@code classpath:db/demo/<module>}. The schema and the history table are created in the module's schema.
     */
    public static Flyway create(String module, ModuleSettings settings, Environment environment) {
        ModuleDataSources.Connection connection = ModuleDataSources.resolve(module, settings, environment);
        List<String> locations = new ArrayList<>(List.of("classpath:db/migration/" + module));
        if (environment.acceptsProfiles(Profiles.of("demo"))) {
            locations.add("classpath:db/demo/" + module);
        }
        return Flyway.configure()
                .dataSource(connection.url(), connection.migrationUsername(), connection.migrationPassword())
                .jdbcProperties(Map.of("ApplicationName", "macro-" + module + "-migration"))
                .locations(locations.toArray(String[]::new))
                .schemas(connection.schema())
                .defaultSchema(connection.schema())
                .createSchemas(true)
                .table("flyway_schema_history")
                .load();
    }
}
