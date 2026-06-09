package com.itr.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CacheConfig — simple in-memory cache manager.
 * Replace with RedisCacheManager when Redis dependency is added to pom.xml.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "pan-validation", "ay-constants", "client-list",
            "ais-data", "form-26as", "itd-session"
        );
    }
}
