package com.securechat.webapi.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class NetworkServiceRegistry {
    private static final Logger log = LoggerFactory.getLogger(NetworkServiceRegistry.class);

    private final Map<String, NetworkServiceStatus> services = new ConcurrentHashMap<>();

    public void registerService(String key, String name, String protocol, String endpoint, String description) {
        services.computeIfAbsent(key, k -> new NetworkServiceStatus(key, name, protocol, endpoint, description));
    }

    public void serviceStarting(String key, String message) {
        withService(key, service -> {
            service.markState(ServiceState.STARTING, message);
            logBanner("BOOT", service, message);
        });
    }

    public void serviceRunning(String key, String message) {
        withService(key, service -> {
            service.markState(ServiceState.RUNNING, message);
            logBanner("RUN", service, message);
        });
    }

    public void serviceDegraded(String key, String message) {
        withService(key, service -> {
            service.markState(ServiceState.DEGRADED, message);
            logBanner("WARN", service, message);
        });
    }

    public void serviceFailed(String key, String message) {
        withService(key, service -> {
            service.markState(ServiceState.FAILED, message);
            logBanner("FAIL", service, message);
        });
    }

    public void serviceDisabled(String key, String reason) {
        withService(key, service -> {
            service.markDisabled(reason);
            logBanner("OFF", service, reason);
        });
    }

    public void recordUsage(String key, String action, String details) {
        withService(key, service -> {
            service.recordUsage(action + " — " + details);
            log.info("🌐 [{}] {} @ {} :: {} — {}", service.protocol(), service.name(), service.endpoint(), action, details);
        });
    }

    public List<NetworkServiceSnapshot> snapshot() {
        return services.values().stream()
                .sorted(Comparator.comparing(NetworkServiceStatus::name))
                .map(NetworkServiceStatus::snapshot)
                .toList();
    }

    private void withService(String key, Consumer<NetworkServiceStatus> consumer) {
        NetworkServiceStatus status = services.computeIfAbsent(key,
                k -> new NetworkServiceStatus(k, key, "-", "-", "(auto-registered)"));
        consumer.accept(status);
    }

    private void logBanner(String phase, NetworkServiceStatus status, String message) {
        log.info("[{}] {} [{}] on {} — {}", phase, status.name(), status.protocol(), status.endpoint(), message);
    }
}
