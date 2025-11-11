package com.securechat.webapi.service;

import com.securechat.webapi.entity.UserEntity;
import com.securechat.webapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PresenceService {
    private static final long ONLINE_THRESHOLD_SECONDS = 10;
    private static final long IDLE_THRESHOLD_SECONDS = 30;

    private final UserRepository userRepository;

    @Autowired
    public PresenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void updatePresence(String userId, String displayName) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElse(new UserEntity());
        user.setUserId(userId);
        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        user.setLastSeen(Instant.now());
        userRepository.save(user);
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
}

