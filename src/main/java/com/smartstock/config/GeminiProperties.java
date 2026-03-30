package com.smartstock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "smartstock.gemini")
public class GeminiProperties {

    private String baseUrl = "https://generativelanguage.googleapis.com";
    private String apiKey;
    private String model = "gemini-3-pro-preview";
    private Duration timeout = Duration.ofSeconds(120);
}
