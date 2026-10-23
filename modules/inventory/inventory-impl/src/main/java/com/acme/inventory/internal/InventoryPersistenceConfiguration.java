package com.acme.inventory.internal;

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

/**
 * Everything this module needs to talk to its own database. JPA was chosen (as requested) with one
 * EntityManagerFactory and one transaction manager per module; no bean is @Primary, so nothing can pick them up by
 * accident.
 */
@ModuleConfiguration(module = "inventory")
@EnableJpaRepositories(basePackages = "com.acme.inventory.internal", entityManagerFactoryRef = "inventoryEmf",
        transactionManagerRef = "inventoryTm")
class InventoryPersistenceConfiguration {

    private static final String MODULE = "inventory";

    @Bean(name = "inventorySettings")
    ModuleSettings inventorySettings(ModuleProperties properties) {
        return properties.of(MODULE);
    }

    @Bean(name = "inventoryDataSource", destroyMethod = "close")
    HikariDataSource inventoryDataSource(@Qualifier("inventorySettings") ModuleSettings settings, Environment environment,
            ObjectProvider<MeterRegistry> registry) {
        return ModuleDataSources.create(MODULE, settings, environment, registry.getIfAvailable());
    }

    @Bean(name = "inventoryFlyway")
    Flyway inventoryFlyway(@Qualifier("inventorySettings") ModuleSettings settings, Environment environment) {
        return ModuleFlyway.create(MODULE, settings, environment);
    }

    @Bean(name = "inventoryMigration")
    ModuleMigration inventoryMigration(@Qualifier("inventoryFlyway") Flyway flyway, Environment environment) {
        return new ModuleMigration(MODULE, flyway,
                environment.getProperty("platform.db.migration-retry-interval", Duration.class, Duration.ofSeconds(5)));
    }

    @Bean(name = "inventoryEmf")
    LocalContainerEntityManagerFactoryBean inventoryEmf(@Qualifier("inventoryDataSource") DataSource dataSource,
            @Qualifier("inventorySettings") ModuleSettings settings, Environment environment) {
        String schema = ModuleDataSources.resolve(MODULE, settings, environment).schema();
        return ModuleJpa.entityManagerFactory(MODULE, dataSource, schema, "com.acme.inventory.internal");
    }

    @Bean(name = "inventoryTm")
    PlatformTransactionManager inventoryTm(@Qualifier("inventoryEmf") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
