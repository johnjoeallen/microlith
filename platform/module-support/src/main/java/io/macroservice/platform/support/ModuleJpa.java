package io.macroservice.platform.support;

import java.util.Map;
import javax.sql.DataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Builds a module's own {@code EntityManagerFactory}. It never connects while starting (no JDBC metadata access). */
public final class ModuleJpa {

    private ModuleJpa() {
    }

    public static LocalContainerEntityManagerFactoryBean entityManagerFactory(String module, DataSource dataSource,
            String schema, String... packagesToScan) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName(module);
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(packagesToScan);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.boot.allow_jdbc_metadata_access", "false",
                "hibernate.default_schema", schema,
                "hibernate.hbm2ddl.auto", "none",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
        return factory;
    }
}
