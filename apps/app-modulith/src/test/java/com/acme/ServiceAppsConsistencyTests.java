package com.acme;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Each app-&lt;module&gt;-service contains its own -impl and exactly the -api artifacts that module may call, and the Maven
 * enforcer rule inside it lists the same set. The source of truth is the module's package-info.java. (The enforcer rule
 * itself rejects any other module artifact at build time; scripts/new-service-app.sh --check detects a stale template output.)
 */
class ServiceAppsConsistencyTests {

    private static final List<String> MODULES = List.of("catalog", "customers", "pricing", "inventory", "payments", "shipping", "notifications", "orders");

    private static Set<String> matches(String text, String regex) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = Pattern.compile(regex).matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    @Test
    void everyServiceAppHasExactlyTheApisItsModuleMayCall() throws Exception {
        for (String module : MODULES) {
            String packageInfo = Files.readString(Path.of("../../modules/%s/%s-impl/src/main/java/com/acme/%s/package-info.java".formatted(module, module, module)));
            Set<String> allowed = matches(packageInfo, "\"([a-z]+)::api\"");
            String pom = Files.readString(Path.of("../app-%s-service/pom.xml".formatted(module)));

            Set<String> declaredApis = matches(pom, "<artifactId>([a-z]+)-api</artifactId></dependency>");
            declaredApis.remove("rpc");
            assertThat(declaredApis).as("-api dependencies of app-%s-service", module).isEqualTo(allowed);

            assertThat(matches(pom, "<artifactId>([a-z]+)-impl</artifactId></dependency>")).as("-impl dependencies of app-%s-service", module)
                    .containsExactly(module);

            Set<String> enforced = matches(pom, "<include>com\\.acme\\.macro:([a-z]+)-api</include>");
            enforced.remove(module);
            enforced.remove("rpc");
            assertThat(enforced).as("-api artifacts allowed by the enforcer rule of app-%s-service", module).isEqualTo(allowed);
            assertThat(pom).contains("<exclude>com.acme.macro:*-impl</exclude>").contains("<exclude>com.acme.macro:*-api</exclude>");
        }
    }
}
