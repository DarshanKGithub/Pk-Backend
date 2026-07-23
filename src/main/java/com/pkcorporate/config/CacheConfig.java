package com.pkcorporate.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Step 6 — Caffeine-backed Spring Cache.
 * <p>
 * Caches the product catalog which is read-heavy and rarely changes.
 * Every cache is bounded by size + TTL so it can never cause an OOM.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Global Caffeine spec for all caches:
     *  - maximumSize 500  : hard cap so the cache never eats unbounded heap
     *  - expireAfterWrite 10 min : product catalog stays fresh enough
     *  - recordStats : useful during dev; harmless in prod
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }
}
