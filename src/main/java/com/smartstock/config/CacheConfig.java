package com.smartstock.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCache("stockDetail", Duration.ofSeconds(2), 2000),
                buildCache("marketSnapshots", Duration.ofSeconds(1), 200),
                buildCache("kline", Duration.ofSeconds(15), 2000),
                buildCache("indicators", Duration.ofSeconds(15), 2000),
                buildCache("searchStocks", Duration.ofMinutes(5), 1000),
                buildCache("flashNews", Duration.ofSeconds(30), 500)
        ));
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .build());
    }
}
