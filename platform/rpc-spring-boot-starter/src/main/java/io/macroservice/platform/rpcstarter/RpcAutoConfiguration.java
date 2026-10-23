package io.macroservice.platform.rpcstarter;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import io.macroservice.platform.support.ModuleProperties;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration
@EnableConfigurationProperties(RpcServerProperties.class)
public class RpcAutoConfiguration {

    @Bean
    static RpcApiRegistrar rpcApiRegistrar(Environment environment) {
        return new RpcApiRegistrar(environment);
    }

    @Bean
    MdcHeaderPropagator mdcHeaderPropagator() {
        return new MdcHeaderPropagator();
    }

    @Bean
    RpcClientProxyFactory rpcClientProxyFactory(ObjectProvider<JsonMapper> mapper,
            ObjectProvider<RpcHeaderPropagator> propagators) {
        return new RpcClientProxyFactory(mapper.getIfAvailable(() -> JsonMapper.builder().build()),
                propagators.orderedStream().toList());
    }

    @Bean
    ModuleBulkhead moduleBulkhead(ModuleProperties properties) {
        return new ModuleBulkhead(properties);
    }

    @Bean
    RpcDispatcher rpcDispatcher(ObjectProvider<JsonMapper> mapper, ListableBeanFactory beans,
            ObjectProvider<RpcServerInterceptor> interceptors) {
        return new RpcDispatcher(mapper.getIfAvailable(() -> JsonMapper.builder().build()), beans,
                interceptors.orderedStream().toList());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnProperty(name = "rpc.server.enabled", matchIfMissing = true)
    static class ServerConfiguration {

        @Bean
        RpcController rpcController(RpcDispatcher dispatcher, ObjectProvider<JsonMapper> mapper) {
            return new RpcController(dispatcher, mapper.getIfAvailable(() -> JsonMapper.builder().build()));
        }

        @Bean
        @ConditionalOnProperty("rpc.server.address")
        FilterRegistrationBean<RpcAddressFilter> rpcAddressFilter(RpcServerProperties properties) throws IOException {
            FilterRegistrationBean<RpcAddressFilter> registration = new FilterRegistrationBean<>(new RpcAddressFilter(properties.address()));
            registration.addUrlPatterns("/rpc/*");
            return registration;
        }
    }
}
