package com.smartstock.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class WebClientRemoteHttpClient implements RemoteHttpClient {

    private final WebClient webClient;

    @Override
    @Retry(name = "remoteSource", fallbackMethod = "fallback")
    @CircuitBreaker(name = "remoteSource", fallbackMethod = "fallback")
    @RateLimiter(name = "remoteSource", fallbackMethod = "fallback")
    @Bulkhead(name = "remoteSource", fallbackMethod = "fallback")
    public String get(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));
    }

    public String fallback(String url, Throwable throwable) {
        log.warn("Remote request failed after resilience fallback, url: {}, error: {}", url, throwable.getMessage());
        return null;
    }
}
