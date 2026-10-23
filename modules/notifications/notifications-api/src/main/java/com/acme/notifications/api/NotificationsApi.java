package com.acme.notifications.api;

import io.macroservice.platform.rpc.RpcApi;
import io.macroservice.platform.rpc.Idempotent;
import java.util.Map;

@RpcApi("notifications")
public interface NotificationsApi {

    /** Records the message in the module's own schema; idempotent by requestId. */
    @Idempotent
    Notification send(String requestId, String customerId, String template, Map<String, String> params);
}
