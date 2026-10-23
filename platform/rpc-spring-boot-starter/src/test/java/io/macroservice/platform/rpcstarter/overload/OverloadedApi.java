package io.macroservice.platform.rpcstarter.overload;

import io.macroservice.platform.rpc.RpcApi;

@RpcApi("over")
public interface OverloadedApi {

    String find(String id);

    String find(String id, int version);
}
