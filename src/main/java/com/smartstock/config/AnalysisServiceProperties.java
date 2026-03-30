package com.smartstock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "smartstock.analysis")
public class AnalysisServiceProperties {

    private String baseUrl = "http://127.0.0.1:18080";
    private Duration timeout = Duration.ofSeconds(180);
}
