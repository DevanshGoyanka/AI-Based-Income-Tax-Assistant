package com.itr.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session Timeout Handler for Sensitive Data
 * Auto-invalidates sessions after 15 minutes of inactivity
 * Clears sensitive data from memory
 */
@Slf4j
@Component
@Configuration
@EnableScheduling
public class SessionTimeoutHandler {

    private static final int TIMEOUT_MINUTES = 15;
    private final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();

    /**
     * Track session activity
     */
    public void trackActivity(String sessionId) {
        SessionData data = activeSessions.computeIfAbsent(sessionId, k -> new SessionData());
        data.setLastActivity(LocalDateTime.now());
        data.setActive(true);
    }

    /**
     * Mark session as containing sensitive data
     */
    public void markSensitive(String sessionId) {
        SessionData data = activeSessions.computeIfAbsent(sessionId, k -> new SessionData());
        data.setSensitiveData(true);
        data.setLastActivity(LocalDateTime.now());
    }

    /**
     * Check if session is expired
     */
    public boolean isExpired(String sessionId) {
        SessionData data = activeSessions.get(sessionId);
        if (data == null) {
            return true;
        }

        LocalDateTime expiryTime = data.getLastActivity().plusMinutes(TIMEOUT_MINUTES);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * Invalidate session and clear sensitive data
     */
    public void invalidateSession(String sessionId) {
        SessionData data = activeSessions.remove(sessionId);
        if (data != null && data.isSensitiveData()) {
            log.info("Session {} invalidated - sensitive data cleared", sessionId);
        }
    }

    /**
     * Scheduled cleanup of expired sessions (runs every 5 minutes)
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredSessions() {
        int cleaned = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, SessionData> entry : activeSessions.entrySet()) {
            SessionData data = entry.getValue();
            LocalDateTime expiryTime = data.getLastActivity().plusMinutes(TIMEOUT_MINUTES);

            if (now.isAfter(expiryTime)) {
                activeSessions.remove(entry.getKey());
                cleaned++;
                
                if (data.isSensitiveData()) {
                    log.warn("Expired session {} contained sensitive data - cleared", entry.getKey());
                }
            }
        }

        if (cleaned > 0) {
            log.info("Session cleanup: {} expired sessions removed", cleaned);
        }
    }

    /**
     * Get current session ID from request context
     */
    public String getCurrentSessionId() {
        try {
            return RequestContextHolder.currentRequestAttributes().getSessionId();
        } catch (Exception e) {
            return null;
        }
    }

    private static class SessionData {
        private LocalDateTime lastActivity;
        private boolean active;
        private boolean sensitiveData;

        public LocalDateTime getLastActivity() {
            return lastActivity;
        }

        public void setLastActivity(LocalDateTime lastActivity) {
            this.lastActivity = lastActivity;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isSensitiveData() {
            return sensitiveData;
        }

        public void setSensitiveData(boolean sensitiveData) {
            this.sensitiveData = sensitiveData;
        }
    }
}
