package io.macroservice.platform.support.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One PostgreSQL container per test JVM (started on first use, removed by Testcontainers' reaper), plus helpers that
 * point modules at it. Every call gets fresh schemas, so tests do not see each other's data.
 */
public final class SharedPostgres {

    public static final String IMAGE = "postgres:17-alpine";
    public static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer(IMAGE);

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    static {
        INSTANCE.start();
    }

    private SharedPostgres() {
    }

    /** A new, started container (for tests that need a second instance or a bootstrap script). */
    public static PostgreSQLContainer newContainer() {
        PostgreSQLContainer container = new PostgreSQLContainer(IMAGE);
        container.start();
        return container;
    }

    /** A new container whose database was initialised with a classpath SQL script (for example db/bootstrap.sql). */
    public static PostgreSQLContainer newContainer(String initScript) {
        PostgreSQLContainer container = new PostgreSQLContainer(IMAGE).withInitScript(initScript);
        container.start();
        return container;
    }

    /** Command-line arguments that give each module its own fresh schema on the shared container. */
    public static String[] args(String... modules) {
        return args(INSTANCE, modules);
    }

    public static String[] args(PostgreSQLContainer container, String... modules) {
        List<String> args = new ArrayList<>();
        int run = SEQUENCE.incrementAndGet();
        for (String module : modules) {
            String prefix = "--modules." + module + ".datasource.";
            args.add(prefix + "url=" + container.getJdbcUrl());
            args.add(prefix + "username=" + container.getUsername());
            args.add(prefix + "password=" + container.getPassword());
            args.add(prefix + "migration-username=" + container.getUsername());
            args.add(prefix + "migration-password=" + container.getPassword());
            args.add(prefix + "schema=" + module + "_" + run);
        }
        return args.toArray(String[]::new);
    }

    /** For {@code @DynamicPropertySource}. */
    public static void register(DynamicPropertyRegistry registry, String... modules) {
        int run = SEQUENCE.incrementAndGet();
        for (String module : modules) {
            String prefix = "modules." + module + ".datasource.";
            registry.add(prefix + "url", INSTANCE::getJdbcUrl);
            registry.add(prefix + "username", INSTANCE::getUsername);
            registry.add(prefix + "password", INSTANCE::getPassword);
            registry.add(prefix + "migration-username", INSTANCE::getUsername);
            registry.add(prefix + "migration-password", INSTANCE::getPassword);
            registry.add(prefix + "schema", () -> module + "_" + run);
        }
    }
}
