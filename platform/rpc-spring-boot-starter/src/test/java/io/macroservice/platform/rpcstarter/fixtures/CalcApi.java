package io.macroservice.platform.rpcstarter.fixtures;

import io.macroservice.platform.rpc.RpcApi;

@RpcApi("calc")
public interface CalcApi {

    int add(int a, int b);

    Echo echo(Echo echo);

    void fire(String message);

    String boom();

    String denied(String who);

    String nullable(String text);
}
