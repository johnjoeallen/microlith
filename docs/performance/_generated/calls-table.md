| Call (mean ms, all bindings) | modulith | loopback-codec | loopback-http | groups | microservices |
| --- | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 1.16 | 1.34 | 2.87 | 2.30 | 4.35 |
| orders -> shipping.createShipment | 1.62 | 1.74 | 2.35 | 2.31 | 3.34 |
| orders -> inventory.reserve | 1.43 | 1.53 | 1.74 | 2.09 | 2.27 |
| orders -> payments.authorize | 1.00 | 1.10 | 1.68 | 1.75 | 2.72 |
| orders -> pricing.quote | 0.78 | 0.88 | 1.70 | 1.91 | 2.85 |
| shipping -> inventory.getStock | 0.40 | 0.43 | 0.89 | 0.44 | 1.39 |
| orders -> notifications.send | 0.79 | 0.88 | 1.59 | 1.21 | 2.58 |
| orders -> payments.capture | 0.62 | 0.67 | 1.02 | 1.25 | 1.47 |
| shipping -> inventory.getReservation | 0.35 | 0.43 | 0.76 | 0.42 | 1.12 |
| orders -> customers.getCustomer | 0.22 | 0.27 | 0.53 | 0.67 | 0.74 |
| pricing -> customers.getCustomer | 0.21 | 0.25 | 0.48 | 0.64 | 0.77 |
| shipping -> customers.getCustomer | 0.12 | 0.13 | 0.24 | 0.33 | 0.35 |
