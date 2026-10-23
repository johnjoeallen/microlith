package io.macroservice.platform.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.macroservice.platform.support.ModuleSettings.Binding;
import io.macroservice.platform.support.fixtures.enabled.EnabledFixtureConfig;
import io.macroservice.platform.support.fixtures.enabled.FooApi;
import io.macroservice.platform.support.fixtures.enabled.FooComponent;
import io.macroservice.platform.support.fixtures.noimpl.NoImplFixtureConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ModuleSupportTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ModuleSupportAutoConfiguration.class));

    @Test
    void disabledModuleHasNoBeans() {
        runner.withUserConfiguration(EnabledFixtureConfig.class)
                .withPropertyValues("modules.foo.url=http://localhost:1/rpc/foo")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(FooApi.class);
                    assertThat(context).doesNotHaveBean(FooComponent.class);
                    assertThat(context).doesNotHaveBean("fooMarker");
                });
    }

    @Test
    void explicitlyDisabledModuleHasNoBeans() {
        runner.withUserConfiguration(EnabledFixtureConfig.class)
                .withPropertyValues("modules.foo.enabled=false", "modules.foo.url=http://localhost:1/rpc/foo")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(FooApi.class));
    }

    @Test
    void enabledModuleHasAllItsBeans() {
        runner.withUserConfiguration(EnabledFixtureConfig.class)
                .withPropertyValues("modules.foo.enabled=true", "spring.datasource.url=jdbc:postgresql://localhost/x")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FooApi.class).hasSingleBean(FooComponent.class);
                    assertThat(context).hasBean("fooMarker");
                });
    }

    @Test
    void settingsBindFromProperties() {
        runner.withPropertyValues(
                "modules.foo.enabled=true", "modules.foo.binding=loopback-codec", "modules.foo.url=http://h/rpc/foo",
                "modules.foo.datasource.url=jdbc:postgresql://db/foo", "modules.foo.datasource.schema=foo")
                .run(context -> {
                    ModuleSettings foo = context.getBean(ModuleProperties.class).of("foo");
                    assertThat(foo.enabled()).isTrue();
                    assertThat(foo.binding()).isEqualTo(Binding.LOOPBACK_CODEC);
                    assertThat(foo.url()).isEqualTo("http://h/rpc/foo");
                    assertThat(foo.datasource().schema()).isEqualTo("foo");
                    assertThat(context.getBean(ModuleProperties.class).of("unknown").enabled()).isFalse();
                });
    }

    @Test
    void disabledWithoutUrlFailsStartup() {
        runner.withUserConfiguration(EnabledFixtureConfig.class).run(context -> assertFailure(context.getStartupFailure(),
                "Module 'foo' is disabled but modules.foo.url is not set"));
    }

    @Test
    void enabledWithoutDatasourceFailsStartup() {
        runner.withUserConfiguration(EnabledFixtureConfig.class).withPropertyValues("modules.foo.enabled=true")
                .run(context -> assertFailure(context.getStartupFailure(),
                        "Module 'foo' is enabled but has no datasource: set modules.foo.datasource.url or spring.datasource.url"));
    }

    @Test
    void enabledWithOwnDatasourceStarts() {
        runner.withUserConfiguration(EnabledFixtureConfig.class)
                .withPropertyValues("modules.foo.enabled=true", "modules.foo.datasource.url=jdbc:postgresql://db/foo")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void enabledWithoutImplementationFailsStartup() {
        runner.withUserConfiguration(NoImplFixtureConfig.class)
                .withPropertyValues("modules.bar.enabled=true", "spring.datasource.url=jdbc:postgresql://localhost/x")
                .run(context -> assertFailure(context.getStartupFailure(),
                        "Module 'bar' is enabled but no implementation of BarApi is on the classpath"));
    }

    @Test
    void disabledWithUrlAndNoImplementationStarts() {
        runner.withUserConfiguration(NoImplFixtureConfig.class)
                .withPropertyValues("modules.bar.url=http://localhost:1/rpc/bar")
                .run(context -> assertThat(context).hasNotFailed());
    }

    private static void assertFailure(Throwable failure, String message) {
        assertThat(failure).isNotNull();
        Throwable root = failure;
        while (root.getCause() != null && !(root instanceof ModuleConfigurationException)) {
            root = root.getCause();
        }
        assertThat(root).isInstanceOf(ModuleConfigurationException.class).hasMessageContaining(message);
    }
}
