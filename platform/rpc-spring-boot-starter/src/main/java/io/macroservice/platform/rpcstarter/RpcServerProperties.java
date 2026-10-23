package io.macroservice.platform.rpcstarter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param enabled whether this process serves {@code /rpc/**}
 * @param address when set, {@code /rpc/**} answers only requests that arrive on this local address (for example
 *                {@code 127.0.0.1}); other requests get a 404
 */
@ConfigurationProperties("rpc.server")
public record RpcServerProperties(@DefaultValue("true") boolean enabled, String address) {
}
