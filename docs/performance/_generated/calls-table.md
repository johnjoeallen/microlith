| Call (mean ms, all bindings) | modulith | loopback-codec | loopback-http | groups | microservices |
| --- | ---: | ---: | ---: | ---: | ---: |
| orders -> shipping.quote | 0.70 | 0.74 | 1.80 | 1.89 | 4.69 |
| orders -> shipping.createShipment | 1.14 | 1.17 | 1.72 | 1.96 | 3.52 |
| orders -> inventory.reserve | 1.20 | 1.18 | 1.47 | 1.97 | 2.35 |
| orders -> payments.authorize | 0.75 | 0.78 | 1.24 | 1.76 | 2.95 |
| orders -> pricing.quote | 0.44 | 0.47 | 1.05 | 1.60 | 3.46 |
| shipping -> inventory.getStock | 0.25 | 0.25 | 0.57 | 0.33 | 1.55 |
| orders -> notifications.send | 0.58 | 0.62 | 1.09 | 0.92 | 2.47 |
| orders -> payments.capture | 0.52 | 0.53 | 0.79 | 1.20 | 1.52 |
| shipping -> inventory.getReservation | 0.27 | 0.29 | 0.55 | 0.37 | 1.11 |
| pricing -> customers.getCustomer | 0.14 | 0.15 | 0.30 | 0.64 | 0.89 |
| orders -> customers.getCustomer | 0.14 | 0.15 | 0.34 | 0.59 | 0.78 |
| shipping -> customers.getCustomer | 0.07 | 0.07 | 0.15 | 0.32 | 0.38 |
