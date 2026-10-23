package com.acme;

import io.macroservice.platform.support.testing.SharedPostgres;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Starts real application contexts for tests and closes them afterwards. */
final class TestApps implements AutoCloseable {

    static final List<String> MODULES = List.of("catalog", "customers", "pricing", "inventory", "payments", "shipping", "notifications", "orders");

    private final List<ConfigurableApplicationContext> contexts = new ArrayList<>();

    ConfigurableApplicationContext start(boolean web, String[]... argumentGroups) {
        String[] args = Arrays.stream(argumentGroups).flatMap(Arrays::stream).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(web ? WebApplicationType.SERVLET : WebApplicationType.NONE).run(args);
        contexts.add(context);
        return context;
    }

    static String[] args(String... args) {
        return args;
    }

    static String[] demo() {
        return args("--spring.profiles.active=demo");
    }

    /**
     * The modules in {@code local} run in this process; every other module is disabled and reached at
     * {@code remoteUrls} (or at a dummy address nobody calls). Never repeats a property, because a repeated
     * command-line argument is joined, not replaced.
     */
    static String[] topology(Map<String, String> remoteUrls, String... local) {
        List<String> localModules = List.of(local);
        List<String> args = new ArrayList<>();
        for (String module : MODULES) {
            if (localModules.contains(module)) {
                args.add("--modules." + module + ".enabled=true");
            } else {
                args.add("--modules." + module + ".enabled=false");
                args.add("--modules." + module + ".url=" + remoteUrls.getOrDefault(module, "http://127.0.0.1:1/rpc/" + module));
            }
        }
        return args.toArray(String[]::new);
    }

    /** Demo profile, the given modules local, each with a fresh schema on the shared container. */
    static String[] local(String... modules) {
        return concat(demo(), topology(Map.of(), modules), SharedPostgres.args(modules));
    }

    static String[] allLocal() {
        return local(MODULES.toArray(String[]::new));
    }

    static String[] concat(String[]... groups) {
        return Arrays.stream(groups).flatMap(Arrays::stream).toArray(String[]::new);
    }

    static String[] binding(String binding, List<String> modules) {
        return modules.stream().map(m -> "--modules." + m + ".binding=" + binding).toArray(String[]::new);
    }

    @Override
    public void close() {
        contexts.reversed().forEach(ConfigurableApplicationContext::close);
        contexts.clear();
    }
}
