package com.acme;

import io.macroservice.demo.scenarios.OrderScenarios;
import io.macroservice.platform.support.FaultControl;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.context.ConfigurableApplicationContext;

/** Probe and fault switches over one or several application contexts (one per process of the deployment). */
final class ScenarioSupport {

    private ScenarioSupport() {
    }

    static OrderScenarios.Probe probe(List<ConfigurableApplicationContext> contexts) {
        return (module, sql, argument) -> {
            DataSource dataSource = contexts.stream().filter(c -> c.containsBean(module + "DataSource"))
                    .map(c -> c.getBean(module + "DataSource", DataSource.class)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No process hosts module " + module));
            try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, argument);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    static OrderScenarios.Faults faults(List<ConfigurableApplicationContext> contexts) {
        return down -> contexts.forEach(c -> c.getBeanProvider(FaultControl.class)
                .ifAvailable(f -> f.setFlag("notifications.down", down)));
    }
}
