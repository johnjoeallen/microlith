package com.acme.orders.internal;

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
@ModuleConfiguration(module = "orders")
@EnableJpaRepositories(basePackages = "com.acme.orders.internal", entityManagerFactoryRef = "ordersEmf",
        transactionManagerRef = "ordersTm")
class OrdersPersistenceConfiguration {

    private static final String MODULE = "orders";

    @Bean(name = "ordersSettings")
    ModuleSettings ordersSettings(ModuleProperties properties) {
        return properties.of(MODULE);
    }

    @Bean(name = "ordersDataSource", destroyMethod = "close")
    HikariDataSource ordersDataSource(@Qualifier("ordersSettings") ModuleSettings settings, Environment environment,
            ObjectProvider<MeterRegistry> registry) {
        return ModuleDataSources.create(MODULE, settings, environment, registry.getIfAvailable());
    }

    @Bean(name = "ordersFlyway")
    Flyway ordersFlyway(@Qualifier("ordersSettings") ModuleSettings settings, Environment environment) {
        return ModuleFlyway.create(MODULE, settings, environment);
    }

    @Bean(name = "ordersMigration")
    ModuleMigration ordersMigration(@Qualifier("ordersFlyway") Flyway flyway, Environment environment) {
        return new ModuleMigration(MODULE, flyway,
                environment.getProperty("platform.db.migration-retry-interval", Duration.class, Duration.ofSeconds(5)));
    }

    @Bean(name = "ordersEmf")
    LocalContainerEntityManagerFactoryBean ordersEmf(@Qualifier("ordersDataSource") DataSource dataSource,
            @Qualifier("ordersSettings") ModuleSettings settings, Environment environment) {
        String schema = ModuleDataSources.resolve(MODULE, settings, environment).schema();
        return ModuleJpa.entityManagerFactory(MODULE, dataSource, schema, "com.acme.orders.internal");
    }

    @Bean(name = "ordersTm")
    PlatformTransactionManager ordersTm(@Qualifier("ordersEmf") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
