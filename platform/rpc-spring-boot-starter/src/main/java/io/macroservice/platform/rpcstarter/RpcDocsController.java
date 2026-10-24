package io.macroservice.platform.rpcstarter;

import io.macroservice.platform.rpc.RpcErrorCodes;
import io.macroservice.platform.rpcdocs.ApiDocs;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Serves the generated OpenAPI and OpenRPC documents of the modules this process runs locally. A module that is
 * only called remotely is documented by the process that serves it. The documents are a function of the API
 * interfaces, so each is built once.
 */
@RestController
@RequestMapping("/rpc")
class RpcDocsController {

    private final RpcDispatcher dispatcher;
    private final JsonMapper mapper;
    private final ApiDocs docs;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    RpcDocsController(RpcDispatcher dispatcher, JsonMapper mapper, String version) {
        this.dispatcher = dispatcher;
        this.mapper = mapper;
        this.docs = new ApiDocs(mapper, version);
    }

    @GetMapping("/openapi.json")
    ResponseEntity<String> mergedOpenApi() {
        return merged("openapi", "Macro service API");
    }

    @GetMapping("/openrpc.json")
    ResponseEntity<String> mergedOpenRpc() {
        return merged("openrpc", "Macro service API");
    }

    @GetMapping("/{module}/openapi.json")
    ResponseEntity<String> openApi(@PathVariable String module) {
        return forModule("openapi", module);
    }

    @GetMapping("/{module}/openrpc.json")
    ResponseEntity<String> openRpc(@PathVariable String module) {
        return forModule("openrpc", module);
    }

    private ResponseEntity<String> merged(String kind, String title) {
        return ok(kind + ":*", () -> generate(kind, title, dispatcher.localApis().values()));
    }

    private ResponseEntity<String> forModule(String kind, String module) {
        Class<?> api = dispatcher.localApis().get(module);
        if (api == null) {
            ObjectNode body = mapper.createObjectNode();
            body.put("code", RpcErrorCodes.METHOD_NOT_FOUND);
            body.put("message", "Module '%s' is not served by this process".formatted(module));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(body));
        }
        return ok(kind + ":" + module, () -> generate(kind, module + " API", List.of(api)));
    }

    private ObjectNode generate(String kind, String title, Collection<Class<?>> apis) {
        return kind.equals("openapi") ? docs.openApi(title, apis) : docs.openRpc(title, apis);
    }

    private ResponseEntity<String> ok(String key, java.util.function.Supplier<ObjectNode> generator) {
        String json = cache.computeIfAbsent(key, k -> mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generator.get()));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }
}
