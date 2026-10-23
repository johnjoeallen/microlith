package com.acme.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.acme.ModuleSlice;
import com.acme.customers.api.Address;
import com.acme.customers.api.Customer;
import com.acme.customers.api.CustomersApi;
import com.acme.customers.api.Tier;
import com.acme.notifications.api.Notification;
import com.acme.notifications.api.NotificationUnavailableException;
import com.acme.notifications.api.NotificationsApi;
import io.macroservice.platform.support.FaultControl;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The notifications module on its own PostgreSQL schema; its collaborators' APIs are mocks, as in a split deployment. */
@ApplicationModuleTest
@ActiveProfiles("demo")
class NotificationsModuleTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        ModuleSlice.properties(registry, "notifications");
    }

    @MockitoBean
    CustomersApi customers;

    @Autowired
    NotificationsApi notifications;

    @Autowired
    FaultControl faults;

    @Test
    void messagesAreRecordedOnceAndAddressedThroughCustomers() {
        when(customers.getCustomer("c")).thenReturn(new Customer("c", "N", "n@example.com", Tier.STANDARD, new Address("1 St", "Town", "IE")));
        Notification first = notifications.send("n-1", "c", "order-confirmed", Map.of("orderId", "o"));
        assertThat(first.email()).isEqualTo("n@example.com");
        assertThat(notifications.send("n-1", "c", "order-confirmed", Map.of("orderId", "o"))).isEqualTo(first);
    }

    @Test
    void theDownFaultMakesTheModuleUnavailable() {
        faults.setFlag("notifications.down", true);
        try {
            assertThatThrownBy(() -> notifications.send("n-2", "c", "t", Map.of())).isExactlyInstanceOf(NotificationUnavailableException.class);
        } finally {
            faults.setFlag("notifications.down", false);
        }
    }
}
