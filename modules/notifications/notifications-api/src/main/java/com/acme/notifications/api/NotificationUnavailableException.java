package com.acme.notifications.api;

import io.macroservice.platform.rpc.RpcError;

@RpcError(code = 6001)
public class NotificationUnavailableException extends RuntimeException {

    public NotificationUnavailableException(String message) {
        super(message);
    }
}
