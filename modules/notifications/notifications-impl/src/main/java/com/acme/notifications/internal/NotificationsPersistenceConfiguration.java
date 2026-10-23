package com.acme.notifications.internal;

import com.zaxxer.hikari.HikariDataSource;
import io.macroservice.platform.support.ModuleConfiguration;
import io.macroservice.platform.support.ModuleDataSources;
import io.macroservice.platform.support.ModuleFlyway;
import io.macroservice.platform.support.ModuleJpa;
import io.macroservice.platform.support.ModuleMigration;
import io.macroservice.platform.support.ModuleProperties;
import io.macroservice.platform.support.ModuleSettings;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/** Everything this module needs to talk to its own database: one pool, Flyway, EntityManagerFactory and transaction manager. */
@ModuleConfiguration(module = "notifications")
@EnableJpaRepositories(basePackages = "com.acme.notifications.internal", entityManagerFactoryRef = "notificationsEmf",
        transactionManagerRef = "notificationsTm")
class NotificationsPersistenceConfiguration {

    private static final String MODULE = "notifications";

    @Bean(name = "notificationsSettings")
    ModuleSettings notificationsSettings(ModuleProperties properties) {
        return properties.of(MODULE);
    }

    @Bean(name = "notificationsDataSource", destroyMethod = "close")
    HikariDataSource notificationsDataSource(@Qualifier("notificationsSettings") ModuleSettings settings, Environment environment,
            ObjectProvider<MeterRegistry> registry) {
        return ModuleDataSources.create(MODULE, settings, environment, registry.getIfAvailable());
    }

    @Bean(name = "notificationsFlyway")
    Flyway notificationsFlyway(@Qualifier("notificationsSettings") ModuleSettings settings, Environment environment) {
        return ModuleFlyway.create(MODULE, settings, environment);
    }

    @Bean(name = "notificationsMigration")
    ModuleMigration notificationsMigration(@Qualifier("notificationsFlyway") Flyway flyway, Environment environment) {
        return new ModuleMigration(MODULE, flyway,
                environment.getProperty("platform.db.migration-retry-interval", Duration.class, Duration.ofSeconds(5)));
    }

    @Bean(name = "notificationsEmf")
    LocalContainerEntityManagerFactoryBean notificationsEmf(@Qualifier("notificationsDataSource") DataSource dataSource,
            @Qualifier("notificationsSettings") ModuleSettings settings, Environment environment) {
        String schema = ModuleDataSources.resolve(MODULE, settings, environment).schema();
        return ModuleJpa.entityManagerFactory(MODULE, dataSource, schema, "com.acme.notifications.internal");
    }

    @Bean(name = "notificationsTm")
    PlatformTransactionManager notificationsTm(@Qualifier("notificationsEmf") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
