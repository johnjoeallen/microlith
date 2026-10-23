package io.macroservice.platform.support;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Migrates one module's schema once the application context is up. A database that is unreachable (or a failing
 * migration) does not stop the application: the module reports not-ready and the migration is retried in the
 * background until it succeeds. Readiness, not liveness, reflects this state.
 */
public class ModuleMigration implements SmartInitializingSingleton, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ModuleMigration.class);

    private final String module;
    private final Flyway flyway;
    private final Duration retryInterval;
    private volatile boolean migrated;
    private volatile String lastError;
    private ScheduledExecutorService retries;

    public ModuleMigration(String module, Flyway flyway, Duration retryInterval) {
        this.module = module;
        this.flyway = flyway;
        this.retryInterval = retryInterval;
    }

    public String module() {
        return module;
    }

    public boolean isMigrated() {
        return migrated;
    }

    /** The message of the last failed attempt, or null. */
    public String lastError() {
        return lastError;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!tryMigrate()) {
            synchronized (this) {
                retries = Executors.newSingleThreadScheduledExecutor(task -> {
                    Thread thread = new Thread(task, "migration-retry-" + module);
                    thread.setDaemon(true);
                    return thread;
                });
                retries.scheduleWithFixedDelay(() -> {
                    if (tryMigrate()) {
                        retries.shutdown();
                    }
                }, retryInterval.toMillis(), retryInterval.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    synchronized boolean tryMigrate() {
        if (migrated) {
            return true;
        }
        try {
            flyway.migrate();
            migrated = true;
            lastError = null;
            log.info("Module '{}' schema is migrated", module);
            return true;
        } catch (RuntimeException e) {
            lastError = rootMessage(e);
            log.warn("Module '{}' is not ready: migration failed ({}); retrying every {} ms", module, lastError, retryInterval.toMillis());
            return false;
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage().lines().findFirst().orElse("");
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (retries != null) {
                retries.shutdownNow();
            }
        }
    }
}
