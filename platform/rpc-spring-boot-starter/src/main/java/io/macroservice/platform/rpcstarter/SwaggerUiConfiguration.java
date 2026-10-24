package io.macroservice.platform.rpcstarter;

import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Swagger UI from the swagger-ui webjar, only in the {@code dev} profile. It lists the merged document and one
 * document per module served by this process, and calls the alias paths {@code /rpc/{module}/{method}}.
 */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
class SwaggerUiConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SwaggerUiConfiguration.class);
    private static final String WEBJAR = "classpath*:META-INF/resources/webjars/swagger-ui/*/swagger-ui-bundle.js";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Resource[] bundles = new PathMatchingResourcePatternResolver().getResources(WEBJAR);
            if (bundles.length == 0) {
                log.warn("Swagger UI is not served: the swagger-ui webjar is not on the classpath");
                return;
            }
            String location = bundles[0].getURL().toString();
            registry.addResourceHandler("/swagger-ui/**").addResourceLocations(location.substring(0, location.lastIndexOf('/') + 1));
        } catch (IOException e) {
            log.warn("Swagger UI is not served: {}", e.getMessage());
        }
    }

    @Controller
    static class SwaggerUiController {

        private final RpcDispatcher dispatcher;

        SwaggerUiController(RpcDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @GetMapping({"/swagger-ui", "/swagger-ui/"})
        String index() {
            return "redirect:/swagger-ui/index.html";
        }

        /** Replaces the webjar's initializer (which points at a demo petstore) with the documents of this process. */
        @GetMapping("/swagger-ui/swagger-initializer.js")
        ResponseEntity<String> initializer() {
            String urls = dispatcher.localApis().keySet().stream()
                    .map(module -> "{url: '/rpc/%s/openapi.json', name: '%s'}".formatted(module, module))
                    .collect(Collectors.joining(", "));
            String script = """
                    window.onload = function () {
                      window.ui = SwaggerUIBundle({
                        urls: [{url: '/rpc/openapi.json', name: 'all modules in this process'}%s],
                        'urls.primaryName': 'all modules in this process',
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        tryItOutEnabled: true,
                        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                        layout: 'StandaloneLayout'
                      });
                    };
                    """.formatted(urls.isEmpty() ? "" : ", " + urls);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/javascript")).body(script);
        }
    }
}
