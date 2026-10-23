package io.macroservice.platform.support.fixtures.enabled;

import io.macroservice.platform.support.ModuleService;

@ModuleService(module = "foo")
public class FooService implements FooApi {
    public String hello() {
        return "hello";
    }
}
