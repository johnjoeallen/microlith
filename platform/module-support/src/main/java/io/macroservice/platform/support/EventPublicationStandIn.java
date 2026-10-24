package io.macroservice.platform.support;

import java.lang.reflect.Proxy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.core.EventPublicationRepository;

/**
 * Spring Modulith's event publication auto-configuration refuses to start without an {@code EventPublicationRepository}, and it is
 * on the classpath of every process that contains a module using it (orders). The module that publishes events provides the real
 * repository; a process that does not host that module gets this stand-in, which fails loudly if anything ever stores an event there.
 */
@AutoConfiguration(beforeName = "org.springframework.modulith.events.config.EventPublicationAutoConfiguration")
@ConditionalOnClass(name = "org.springframework.modulith.events.core.EventPublicationRepository")
class EventPublicationStandIn {

    @Bean
    @ConditionalOnMissingBean(EventPublicationRepository.class)
    EventPublicationRepository noEventPublicationRepository() {
        return (EventPublicationRepository) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {EventPublicationRepository.class}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "no event publication repository (the module that stores events is not hosted here)";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            throw new IllegalStateException("Events are stored by the module that publishes them, which is not enabled in this process");
        });
    }
}
