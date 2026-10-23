package com.acme.orders.internal;

import java.util.Map;

/** A call to another module that must happen eventually; becomes one outbox row. */
record OutboxCommand(String target, String requestId, Map<String, String> args) {
}
