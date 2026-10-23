package io.macroservice.platform.demo;

import io.macroservice.platform.support.FaultControl;
import io.macroservice.platform.support.ModuleProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
class DemoController {

    private final Topology topology;
    private final TraceStore traces;
    private final FaultControl faults;
    private final ModuleProperties properties;

    DemoController(Topology topology, TraceStore traces, FaultControl faults, ModuleProperties properties) {
        this.topology = topology;
        this.traces = traces;
        this.faults = faults;
        this.properties = properties;
    }

    @GetMapping("/topology")
    Map<String, Object> topology() {
        return topology.describe();
    }

    /** The cross-module calls this process recorded for one correlation id, in start order. */
    @GetMapping("/trace")
    ResponseEntity<List<TraceRecord>> trace(@RequestParam String correlationId) {
        return ResponseEntity.ok(traces.find(correlationId));
    }

    @GetMapping("/faults")
    Map<String, Object> faults() {
        return Map.of("state", faults.snapshot());
    }

    /**
     * Turns demo failures on or off: {@code {"notificationsDown": true, "paymentsDecline": false, "inventoryLatencyMs": 300}}.
     * A switch only applies when its module is enabled in this process; the others are reported as ignored.
     */
    @PostMapping("/faults")
    Map<String, Object> setFaults(@RequestBody Map<String, Object> request) {
        Map<String, Object> applied = new LinkedHashMap<>();
        Map<String, Object> ignored = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : request.entrySet()) {
            String module = switch (entry.getKey()) {
                case "notificationsDown" -> "notifications";
                case "paymentsDecline" -> "payments";
                case "inventoryLatencyMs" -> "inventory";
                default -> null;
            };
            if (module == null) {
                ignored.put(entry.getKey(), "unknown switch");
            } else if (!properties.of(module).enabled()) {
                ignored.put(entry.getKey(), "module " + module + " is not enabled in this process");
            } else {
                switch (entry.getKey()) {
                    case "notificationsDown" -> faults.setFlag("notifications.down", Boolean.parseBoolean(String.valueOf(entry.getValue())));
                    case "paymentsDecline" -> faults.setFlag("payments.decline", Boolean.parseBoolean(String.valueOf(entry.getValue())));
                    default -> faults.setDelay("inventory.latency", Duration.ofMillis(Long.parseLong(String.valueOf(entry.getValue()))));
                }
                applied.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.of("applied", applied, "ignored", ignored, "state", faults.snapshot());
    }
}
