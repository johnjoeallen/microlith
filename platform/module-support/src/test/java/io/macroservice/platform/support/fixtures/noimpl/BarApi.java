package io.macroservice.platform.support.fixtures.noimpl;

import io.macroservice.platform.rpc.RpcApi;

@RpcApi("bar")
public interface BarApi {
    String hello();
}
