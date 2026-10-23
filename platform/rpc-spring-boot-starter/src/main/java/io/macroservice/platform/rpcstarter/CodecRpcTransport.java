package io.macroservice.platform.rpcstarter;

import java.util.Map;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** In-process transport: the full JSON codec and the dispatcher, without HTTP. */
public class CodecRpcTransport implements RpcTransport {

    private final Supplier<RpcDispatcher> dispatcher;
    private final JsonMapper mapper;

    public CodecRpcTransport(Supplier<RpcDispatcher> dispatcher, JsonMapper mapper) {
        this.dispatcher = dispatcher;
        this.mapper = mapper;
    }

    @Override
    public String send(String module, String requestJson, Map<String, String> headers) {
        JsonNode response = dispatcher.get().handleEnvelope(module, requestJson);
        return response == null ? null : mapper.writeValueAsString(response);
    }
}
