package io.macroservice.platform.support;

import java.util.LinkedHashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** All {@code modules.<name>.*} settings, keyed by module name. */
@ConfigurationProperties("modules")
public class ModuleProperties extends LinkedHashMap<String, ModuleSettings> {

    /** Settings of a module; a module with no configuration at all is disabled and has no URL. */
    public ModuleSettings of(String module) {
        return getOrDefault(module, ModuleSettings.absent());
    }
}
