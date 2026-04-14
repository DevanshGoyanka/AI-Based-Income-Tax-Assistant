package com.itr.service;

import com.itr.dto.AISData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AIS Reconciliation Result Caching Service
 * Caches reconciliation results to avoid repeated API calls
 * TTL: 24 hours (AIS data updates daily)
 */
@Slf4j
@Service
public class AISReconciliationCacheService {

    private static final long CACHE_TTL_HOURS = 24;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    /**
     * Get cached reconciliation result
     */
    public CachedResult get(String pan, String assessmentYear) {
        String key = buildKey(pan, assessmentYear);
        CachedResult cached = cache.get(key);

        if (cached != null && !cached.isExpired()) {
            log.debug("AIS cache HIT for PAN: {}, AY: {}", pan, assessmentYear);
            return cached;
        }

        if (cached != null) {
            cache.remove(key);
            log.debug("AIS cache EXPIRED for PAN: {}, AY: {}", pan, assessmentYear);
        }

        return null;
    }

    /**
     * Store reconciliation result in cache
     */
    public void put(String pan, String assessmentYear, AISData aisData, 
                    Object result) {
        String key = buildKey(pan, assessmentYear);
        CachedResult cached = new CachedResult();
        cached.setPan(pan);
        cached.setAssessmentYear(assessmentYear);
        cached.setAisData(aisData);
        cached.setReconciliationResult(result);
        cached.setCachedAt(LocalDateTime.now());
        cached.setExpiresAt(LocalDateTime.now().plusHours(CACHE_TTL_HOURS));

        cache.put(key, cached);
        log.info("AIS cache STORED for PAN: {}, AY: {}", pan, assessmentYear);
    }

    /**
     * Invalidate cache for specific PAN/AY
     */
    public void invalidate(String pan, String assessmentYear) {
        String key = buildKey(pan, assessmentYear);
        cache.remove(key);
        log.info("AIS cache INVALIDATED for PAN: {}, AY: {}", pan, assessmentYear);
    }

    /**
     * Clear all expired entries
     */
    public void cleanupExpired() {
        int removed = 0;
        for (Map.Entry<String, CachedResult> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.info("AIS cache cleanup: {} expired entries removed", removed);
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        CacheStats stats = new CacheStats();
        stats.setTotalEntries(cache.size());
        
        long expired = cache.values().stream()
            .filter(CachedResult::isExpired)
            .count();
        stats.setExpiredEntries((int) expired);
        stats.setActiveEntries(cache.size() - (int) expired);

        return stats;
    }

    private String buildKey(String pan, String assessmentYear) {
        return pan + ":" + assessmentYear;
    }

    @Data
    public static class CachedResult {
        private String pan;
        private String assessmentYear;
        private AISData aisData;
        private Object reconciliationResult;
        private LocalDateTime cachedAt;
        private LocalDateTime expiresAt;

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    @Data
    public static class CacheStats {
        private int totalEntries;
        private int activeEntries;
        private int expiredEntries;
    }
}
