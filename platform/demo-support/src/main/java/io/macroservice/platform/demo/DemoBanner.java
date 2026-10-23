package io.macroservice.platform.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/** Prints the topology once the application is up. */
class DemoBanner implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger("macro.topology");

    private final Topology topology;

    DemoBanner(Topology topology) {
        this.topology = topology;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("{}", topology.banner());
    }
}
