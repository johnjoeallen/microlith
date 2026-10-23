package com.acme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Starts real application contexts for tests and closes them afterwards. */
final class TestApps implements AutoCloseable {

    /** Both pilot modules local, demo seed data on. */
    static final String[] ALL_LOCAL = {"--spring.profiles.active=demo", "--modules.orders.enabled=true", "--modules.inventory.enabled=true"};
    /** Orders is not part of the test; its proxy points nowhere. */
    static final String[] ORDERS_ELSEWHERE = {"--spring.profiles.active=demo", "--modules.orders.enabled=false",
            "--modules.orders.url=http://127.0.0.1:1/rpc/orders"};

    private final List<ConfigurableApplicationContext> contexts = new ArrayList<>();

    ConfigurableApplicationContext start(boolean web, String[]... argumentGroups) {
        String[] args = Arrays.stream(argumentGroups).flatMap(Arrays::stream).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(web ? WebApplicationType.SERVLET : WebApplicationType.NONE).run(args);
        contexts.add(context);
        return context;
    }

    static String[] args(String... args) {
        return Stream.of(args).toArray(String[]::new);
    }

    @Override
    public void close() {
        contexts.reversed().forEach(ConfigurableApplicationContext::close);
        contexts.clear();
    }
}
