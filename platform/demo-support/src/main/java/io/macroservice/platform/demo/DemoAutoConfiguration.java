package io.macroservice.platform.demo;

import io.macroservice.platform.support.FaultControl;
import io.macroservice.platform.support.ModuleProperties;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Everything in here exists only in the {@code demo} profile. */
@AutoConfiguration(afterName = "io.macroservice.platform.support.ModuleSupportAutoConfiguration")
@Profile("demo")
public class DemoAutoConfiguration {

    @Bean
    TraceStore traceStore() {
        return new TraceStore(5000);
    }

    @Bean
    static TracingBeanPostProcessor tracingBeanPostProcessor(org.springframework.beans.factory.ObjectProvider<TraceStore> store) {
        return new TracingBeanPostProcessor(new LazyStore(store));
    }

    @Bean
    TraceServerInterceptor traceServerInterceptor() {
        return new TraceServerInterceptor();
    }

    @Bean
    TraceHeaderPropagator traceHeaderPropagator() {
        return new TraceHeaderPropagator();
    }

    @Bean
    Topology topology(ConfigurableListableBeanFactory beanFactory, ModuleProperties properties, Environment environment) {
        return new Topology(beanFactory, properties, environment);
    }

    @Bean
    DemoBanner demoBanner(Topology topology) {
        return new DemoBanner(topology);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = Type.SERVLET)
    static class WebConfiguration {

        @Bean
        DemoController demoController(Topology topology, TraceStore traces, FaultControl faults, ModuleProperties properties) {
            return new DemoController(topology, traces, faults, properties);
        }

        @Bean
        org.springframework.boot.web.servlet.FilterRegistrationBean<TraceDepthFilter> traceDepthFilter() {
            org.springframework.boot.web.servlet.FilterRegistrationBean<TraceDepthFilter> registration =
                    new org.springframework.boot.web.servlet.FilterRegistrationBean<>(new TraceDepthFilter());
            registration.addUrlPatterns("/rpc/*");
            return registration;
        }
    }
}
