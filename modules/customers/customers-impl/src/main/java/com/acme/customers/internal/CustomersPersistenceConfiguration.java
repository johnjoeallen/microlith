package com.acme.customers.internal;

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
@ModuleConfiguration(module = "customers")
@EnableJpaRepositories(basePackages = "com.acme.customers.internal", entityManagerFactoryRef = "customersEmf",
        transactionManagerRef = "customersTm")
class CustomersPersistenceConfiguration {

    private static final String MODULE = "customers";

    @Bean(name = "customersSettings")
    ModuleSettings customersSettings(ModuleProperties properties) {
        return properties.of(MODULE);
    }

    @Bean(name = "customersDataSource", destroyMethod = "close")
    HikariDataSource customersDataSource(@Qualifier("customersSettings") ModuleSettings settings, Environment environment,
            ObjectProvider<MeterRegistry> registry) {
        return ModuleDataSources.create(MODULE, settings, environment, registry.getIfAvailable());
    }

    @Bean(name = "customersFlyway")
    Flyway customersFlyway(@Qualifier("customersSettings") ModuleSettings settings, Environment environment) {
        return ModuleFlyway.create(MODULE, settings, environment);
    }

    @Bean(name = "customersMigration")
    ModuleMigration customersMigration(@Qualifier("customersFlyway") Flyway flyway, Environment environment) {
        return new ModuleMigration(MODULE, flyway,
                environment.getProperty("platform.db.migration-retry-interval", Duration.class, Duration.ofSeconds(5)));
    }

    @Bean(name = "customersEmf")
    LocalContainerEntityManagerFactoryBean customersEmf(@Qualifier("customersDataSource") DataSource dataSource,
            @Qualifier("customersSettings") ModuleSettings settings, Environment environment) {
        String schema = ModuleDataSources.resolve(MODULE, settings, environment).schema();
        return ModuleJpa.entityManagerFactory(MODULE, dataSource, schema, "com.acme.customers.internal");
    }

    @Bean(name = "customersTm")
    PlatformTransactionManager customersTm(@Qualifier("customersEmf") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
