package io.macroservice.platform.support;

/** Thrown at startup when the module configuration of a deployment is inconsistent. */
public class ModuleConfigurationException extends IllegalStateException {

    public ModuleConfigurationException(String message) {
        super(message);
    }
}
