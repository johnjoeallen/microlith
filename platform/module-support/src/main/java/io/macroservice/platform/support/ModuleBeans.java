package io.macroservice.platform.support;

/** Bean-definition attributes shared between module-support and the RPC starter. */
public final class ModuleBeans {

    /** Set on bean definitions of generated RPC client proxies; such beans are never a "local implementation". */
    public static final String CLIENT_PROXY_ATTRIBUTE = "macroservice.rpc.client-proxy";

    private ModuleBeans() {
    }
}
