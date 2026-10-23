@ApplicationModule(allowedDependencies = {"customers::api", "pricing::api", "shipping::api", "inventory::api", "payments::api", "notifications::api"})
package com.acme.orders;

import org.springframework.modulith.ApplicationModule;
