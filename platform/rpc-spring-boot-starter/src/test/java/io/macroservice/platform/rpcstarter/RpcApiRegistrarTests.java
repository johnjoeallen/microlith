package io.macroservice.platform.rpcstarter;

import static org.assertj.core.api.Assertions.assertThat;

import io.macroservice.platform.rpc.RpcClientProxy;
import io.macroservice.platform.rpc.RpcUnavailableException;
import io.macroservice.platform.rpcstarter.fixtures.CalcApi;
import io.macroservice.platform.rpcstarter.fixtures.CalcFixtureConfig;
import io.macroservice.platform.rpcstarter.fixtures.CalcService;
import io.macroservice.platform.rpcstarter.fixtures.DeniedException;
import io.macroservice.platform.rpcstarter.fixtures.Echo;
import io.macroservice.platform.rpcstarter.overload.OverloadFixtureConfig;
import io.macroservice.platform.support.ModuleSupportAutoConfiguration;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RpcApiRegistrarTests {

    private static final String DS = "spring.datasource.url=jdbc:postgresql://localhost/x";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ModuleSupportAutoConfiguration.class, RpcAutoConfiguration.class))
            .withUserConfiguration(CalcFixtureConfig.class);

    private static void assertFailure(Throwable failure, String message) {
        assertThat(failure).isNotNull();
        boolean found = false;
        for (Throwable t = failure; t != null; t = t.getCause()) {
            found |= t.getMessage() != null && t.getMessage().contains(message);
        }
        assertThat(found).as("failure chain of %s should mention '%s'", failure, message).isTrue();
    }

    @Test
    void localImplementationIsInjectedDirectlyByDefault() {
        runner.withPropertyValues("modules.calc.enabled=true", DS).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CalcApi.class)).isInstanceOf(CalcService.class).isNotInstanceOf(RpcClientProxy.class);
            assertThat(context).doesNotHaveBean("calcRpcClient");
        });
    }

    @Test
    void loopbackCodecRoutesLocalCallsThroughTheJsonCodec() {
        runner.withPropertyValues("modules.calc.enabled=true", "modules.calc.binding=loopback-codec", DS).run(context -> {
            assertThat(context).hasNotFailed();
            CalcApi api = context.getBean(CalcApi.class);
            assertThat(api).isInstanceOf(RpcClientProxy.class).isNotInstanceOf(CalcService.class);
            assertThat(api.add(2, 3)).isEqualTo(5);
            assertThat(api.echo(new Echo("a", 1))).isEqualTo(new Echo("a", 1));
            assertThat(api.nullable(null)).isNull();
            api.fire("through-codec");
            assertThat(context.getBean(CalcService.class).fired).containsExactly("through-codec");
            ThrowableAssert.ThrowingCallable denied = () -> api.denied("eve");
            org.assertj.core.api.Assertions.assertThatThrownBy(denied).isExactlyInstanceOf(DeniedException.class).hasMessage("denied: eve");
        });
    }

    @Test
    void disabledModuleGetsARemoteProxyThatReportsUnavailable() {
        runner.withPropertyValues("modules.calc.url=http://127.0.0.1:1/rpc/calc").run(context -> {
            assertThat(context).hasNotFailed();
            CalcApi api = context.getBean(CalcApi.class);
            assertThat(api).isInstanceOf(RpcClientProxy.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> api.add(1, 2)).isInstanceOf(RpcUnavailableException.class);
        });
    }

    @Test
    void missingUrlFailsFast() {
        runner.run(context -> assertFailure(context.getStartupFailure(), "Module 'calc' is disabled but modules.calc.url is not set"));
        runner.withPropertyValues("modules.calc.enabled=true", "modules.calc.binding=remote", DS).run(context ->
                assertFailure(context.getStartupFailure(), "Module 'calc' uses binding remote but modules.calc.url is not set"));
    }

    @Test
    void loopbackIsRejectedInProductionProfiles() {
        for (String profile : new String[] {"prod", "production"}) {
            runner.withPropertyValues("modules.calc.enabled=true", "modules.calc.binding=loopback-codec", DS)
                    .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                    .run(context -> assertFailure(context.getStartupFailure(), "Module 'calc' uses binding loopback-codec, which is not allowed in a production profile"));
        }
    }

    @Test
    void loopbackHttpNeedsTheServerEnabled() {
        runner.withPropertyValues("modules.calc.enabled=true", "modules.calc.binding=loopback-http", "rpc.server.enabled=false", DS)
                .run(context -> assertFailure(context.getStartupFailure(), "Module 'calc' uses binding loopback-http, which needs rpc.server.enabled=true"));
    }

    @Test
    void overloadedApiFailsStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ModuleSupportAutoConfiguration.class, RpcAutoConfiguration.class))
                .withUserConfiguration(OverloadFixtureConfig.class).withPropertyValues("modules.over.url=http://127.0.0.1:1/rpc/over")
                .run(context -> assertFailure(context.getStartupFailure(), "overloaded method 'find'"));
    }
}
