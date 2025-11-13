package com.securechat.webapi.service;

import com.securechat.webapi.entity.UserEntity;
import com.securechat.webapi.repository.UserRepository;
import com.securechat.webapi.telemetry.NetworkServiceKeys;
import com.securechat.webapi.telemetry.NetworkServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class PresenceService {
    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);
    private static final long ONLINE_THRESHOLD_SECONDS = 10;
    private static final long IDLE_THRESHOLD_SECONDS = 30;

    private final UserRepository userRepository;
    private final NetworkServiceRegistry networkServiceRegistry;
    private final int udpPresencePort;
    private final int httpPort;
    private final long summaryLogIntervalMs;
    private final int httpPresenceTelemetryLogEvery;
    private final AtomicLong lastOnlineMembersLogTime = new AtomicLong(0);
    private final AtomicLong httpTelemetryCounter = new AtomicLong(0);

    @Autowired
    public PresenceService(UserRepository userRepository,
                           NetworkServiceRegistry networkServiceRegistry,
                           @Value("${udp.presence.port:9090}") int udpPresencePort,
                           @Value("${server.port:8080}") int httpPort,
                           @Value("${presence.http.telemetry.log-every:30}") int httpPresenceTelemetryLogEvery,
                           @Value("${presence.summary.log-interval-seconds:45}") long summaryLogIntervalSeconds) {
        this.userRepository = userRepository;
        this.networkServiceRegistry = networkServiceRegistry;
        this.udpPresencePort = udpPresencePort;
        this.httpPort = httpPort;
        this.httpPresenceTelemetryLogEvery = Math.max(1, httpPresenceTelemetryLogEvery);
        this.summaryLogIntervalMs = Math.max(1, summaryLogIntervalSeconds) * 1000;
    }

    @Transactional
    public void updatePresence(String userId, String displayName) {
        UserEntity existingUser = userRepository.findByUserId(userId).orElse(null);
        String previousStatus = existingUser != null && existingUser.getLastSeen() != null 
            ? calculateStatus(existingUser.getLastSeen()) : "offline";
        
        UserEntity user = existingUser != null ? existingUser : new UserEntity();
        user.setUserId(userId);
        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        user.setLastSeen(Instant.now());
        userRepository.save(user);
        
        String newStatus = calculateStatus(user.getLastSeen());
        String display = displayName != null ? displayName : userId;
        boolean isStatusChange = !newStatus.equals(previousStatus);
        boolean isUdpBeacon = displayName == null;
        
        // Only show detailed service logs for HTTP heartbeats or when status changes
        if (!isUdpBeacon || isStatusChange) {
            log.info("   └─ [SERVICE] PresenceService.updatePresence() - Updating user presence");
            log.info("      → Using UserRepository (JPA/Hibernate)");
            log.info("      → User presence saved to database");
        }

        if (isUdpBeacon) {
            if (isStatusChange) {
                networkServiceRegistry.recordUsage(
                    NetworkServiceKeys.UDP_PRESENCE_SERVER,
                    "UDP-UPDATE",
                    String.format("%s synchronized from UDP beacon on port %d (status: %s)", display, udpPresencePort, newStatus)
                );
            }
        } else if (shouldEmitHttpTelemetry(isStatusChange)) {
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.UDP_PRESENCE_SERVER,
                "HTTP-HEARTBEAT",
                String.format("%s heartbeat accepted via http://localhost:%d/api/presence/beat", display, httpPort)
            );
        }
        
        // Only log status changes to avoid spam
        // For UDP: Only log when user comes ONLINE (logs in), not on every beacon
        if (isStatusChange) {
            String emoji = newStatus.equals("online") ? "🟢" : newStatus.equals("idle") ? "🟡" : "🔴";
            String source = isUdpBeacon ? "UDP beacon" : "HTTP heartbeat";
            
            // For UDP beacons, only log when coming online (login), not other status changes
            if (isUdpBeacon && !newStatus.equals("online")) {
                // UDP beacon but not coming online - log at debug level
                log.debug("📡 UDP PRESENCE: {} status changed from {} to {} (via UDP beacon)", 
                    display, previousStatus, newStatus);
            } else {
                // HTTP heartbeat or UDP login - log at info level
                log.info("{} PRESENCE UPDATE: {} is now {} (via {})", emoji, display, newStatus.toUpperCase(), source);
                
                // Special log for UDP login
                if (isUdpBeacon && newStatus.equals("online") && previousStatus.equals("offline")) {
                    log.info("📡 UDP MEMBER LOGIN: {} detected via UDP presence beacon from network", display);
                }
            }
        }
        
        // Log online members summary periodically (every 5 seconds)
        // Only log summary for HTTP heartbeats or when status changes, not on every UDP beacon
        if (!isUdpBeacon || isStatusChange) {
            long currentTime = System.currentTimeMillis();
            long lastLogTime = lastOnlineMembersLogTime.get();
            if (currentTime - lastLogTime >= summaryLogIntervalMs) {
                if (lastOnlineMembersLogTime.compareAndSet(lastLogTime, currentTime)) {
                    logOnlineMembers();
                }
            }
        }
        
        // Only show completion log for HTTP heartbeats or when status changes
        if (!isUdpBeacon || isStatusChange) {
            log.info("   └─ [SERVICE] PresenceService.updatePresence() - Completed");
        }
    }
    
    private void logOnlineMembers() {
        Map<String, String> statusMap = getUserPresenceStatus();
        List<String> onlineUsers = statusMap.entrySet().stream()
            .filter(entry -> "online".equals(entry.getValue()))
            .map(entry -> {
                UserEntity user = userRepository.findByUserId(entry.getKey()).orElse(null);
                return user != null && user.getDisplayName() != null ? user.getDisplayName() : entry.getKey();
            })
            .collect(Collectors.toList());
        
        List<String> idleUsers = statusMap.entrySet().stream()
            .filter(entry -> "idle".equals(entry.getValue()))
            .map(entry -> {
                UserEntity user = userRepository.findByUserId(entry.getKey()).orElse(null);
                return user != null && user.getDisplayName() != null ? user.getDisplayName() : entry.getKey();
            })
            .collect(Collectors.toList());
        
        if (!onlineUsers.isEmpty() || !idleUsers.isEmpty()) {
            log.info("👥 [PRESENCE] Online Members Summary:");
            if (!onlineUsers.isEmpty()) {
                log.info("   🟢 ONLINE ({}): {}", onlineUsers.size(), String.join(", ", onlineUsers));
            }
            if (!idleUsers.isEmpty()) {
                log.info("   🟡 IDLE ({}): {}", idleUsers.size(), String.join(", ", idleUsers));
            }
            log.info("   📊 Total active users: {}", onlineUsers.size() + idleUsers.size());
        }
    }
    
    private String calculateStatus(Instant lastSeen) {
        if (lastSeen == null) {
            return "offline";
        }
        long secondsSinceLastSeen = ChronoUnit.SECONDS.between(lastSeen, Instant.now());
        if (secondsSinceLastSeen <= ONLINE_THRESHOLD_SECONDS) {
            return "online";
        } else if (secondsSinceLastSeen <= IDLE_THRESHOLD_SECONDS) {
            return "idle";
        } else {
            return "offline";
        }
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Map<String, String> getUserPresenceStatus() {
        Instant now = Instant.now();
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(
                        UserEntity::getUserId,
                        user -> {
                            if (user.getLastSeen() == null) {
                                return "offline";
                            }
                            long secondsSinceLastSeen = ChronoUnit.SECONDS.between(user.getLastSeen(), now);
                            if (secondsSinceLastSeen <= ONLINE_THRESHOLD_SECONDS) {
                                return "online";
                            } else if (secondsSinceLastSeen <= IDLE_THRESHOLD_SECONDS) {
                                return "idle";
                            } else {
                                return "offline";
                            }
                        }
                ));
    }

    private boolean shouldEmitHttpTelemetry(boolean isStatusChange) {
        if (isStatusChange) {
            return true;
        }
        long sample = httpTelemetryCounter.incrementAndGet();
        return httpPresenceTelemetryLogEvery <= 1 || sample % httpPresenceTelemetryLogEvery == 0;
    }
}




