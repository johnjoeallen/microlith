package io.macroservice.platform.support.fixtures.enabled;

import io.macroservice.platform.rpc.RpcApi;

@RpcApi("foo")
public interface FooApi {
    String hello();
}
