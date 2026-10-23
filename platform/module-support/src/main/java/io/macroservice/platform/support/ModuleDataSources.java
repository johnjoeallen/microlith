package io.macroservice.platform.support;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.env.Environment;

/** Builds the connection settings and the HikariCP pool of one module from {@code modules.<name>.datasource.*}. */
public final class ModuleDataSources {

    private ModuleDataSources() {
    }

    /**
     * Effective connection settings; anything the module does not set falls back to {@code spring.datasource.*}.
     *
     * @param migrationUsername falls back to the runtime username
     */
    public record Connection(String url, String username, String password, String migrationUsername,
            String migrationPassword, String schema) {
    }

    public static Connection resolve(String module, ModuleSettings settings, Environment environment) {
        ModuleSettings.Datasource own = settings.datasource();
        String url = firstNonBlank(own.url(), environment.getProperty("spring.datasource.url"));
        if (url == null) {
            throw new ModuleConfigurationException("Module '%s' has no datasource: set modules.%s.datasource.url or spring.datasource.url"
                    .formatted(module, module));
        }
        String username = firstNonBlank(own.username(), environment.getProperty("spring.datasource.username"));
        String password = firstNonBlank(own.password(), environment.getProperty("spring.datasource.password"));
        String schema = firstNonBlank(own.schema(), module);
        return new Connection(url, username, password, firstNonBlank(own.migrationUsername(), username),
                own.migrationUsername() == null || own.migrationUsername().isBlank() ? password : own.migrationPassword(), schema);
    }

    /**
     * Creates the module's pool. It never opens a connection while starting ({@code minimumIdle} defaults to 0 and
     * {@code initializationFailTimeout} is -1), so an unreachable database does not stop the application.
     *
     * @param registry optional; when present the pool is exposed as metrics tagged by module
     */
    public static HikariDataSource create(String module, ModuleSettings settings, Environment environment, MeterRegistry registry) {
        ModuleSettings.Datasource pool = settings.datasource();
        Connection connection = resolve(module, settings, environment);
        HikariConfig config = new HikariConfig();
        config.setPoolName(module);
        config.setJdbcUrl(connection.url());
        config.setUsername(connection.username());
        config.setPassword(connection.password());
        config.setSchema(connection.schema());
        config.setMaximumPoolSize(pool.maxPoolSize());
        config.setMinimumIdle(pool.minimumIdle());
        config.setIdleTimeout(pool.idleTimeout().toMillis());
        config.setMaxLifetime(pool.maxLifetime().toMillis());
        config.setConnectionTimeout(pool.connectionTimeout().toMillis());
        config.setInitializationFailTimeout(-1);
        config.addDataSourceProperty("ApplicationName", "macro-" + module);
        if (registry != null) {
            config.setMetricsTrackerFactory(new ModulePoolMetrics(module, registry));
        }
        return new HikariDataSource(config);
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second != null && !second.isBlank() ? second : null;
    }
}
