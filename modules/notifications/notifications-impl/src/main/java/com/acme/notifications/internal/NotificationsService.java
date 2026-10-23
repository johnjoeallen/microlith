package com.acme.notifications.internal;

import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomersApi;
import com.acme.notifications.api.Notification;
import com.acme.notifications.api.NotificationUnavailableException;
import com.acme.notifications.api.NotificationsApi;
import io.macroservice.platform.support.FaultControl;
import io.macroservice.platform.support.ModuleService;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/** "Sends" a message by recording it in this module's own schema and logging it; no real email. */
@ModuleService(module = "notifications")
class NotificationsService implements NotificationsApi {

    private static final Logger log = LoggerFactory.getLogger(NotificationsService.class);

    private final MessageRepository messages;
    private final CustomersApi customers;
    private final ObjectProvider<FaultControl> faults;

    NotificationsService(MessageRepository messages, CustomersApi customers, ObjectProvider<FaultControl> faults) {
        this.messages = messages;
        this.customers = customers;
        this.faults = faults;
    }

    @Override
    public Notification send(String requestId, String customerId, String template, Map<String, String> params) {
        if (faults.getIfAvailable() != null && faults.getObject().flag("notifications.down")) {
            throw new NotificationUnavailableException("Notifications are down");
        }
        var existing = messages.findByRequestId(requestId);
        if (existing.isPresent()) {
            return toApi(existing.get());
        }
        Customer customer = customers.getCustomer(customerId);
        MessageEntity message = messages.save(new MessageEntity(UUID.randomUUID().toString(), requestId, customerId, customer.email(),
                template, new TreeMap<>(params == null ? Map.of() : params).toString()));
        log.info("Notification {} '{}' recorded for {}", message.getNotificationId(), template, customer.email());
        return toApi(message);
    }

    private static Notification toApi(MessageEntity e) {
        return new Notification(e.getNotificationId(), e.getCustomerId(), e.getEmail(), e.getTemplate());
    }
}
