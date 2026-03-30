package com.smartstock.config;

import com.smartstock.client.RemoteHttpClient;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class InfrastructureConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    CacheConfig.class,
                    OpenApiConfig.class,
                    WebClientConfig.class
            );

    @Test
    void shouldRegisterBackendInfrastructureBeans() {
        contextRunner.run((context) -> {
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context).hasSingleBean(OpenAPI.class);
            assertThat(context).hasSingleBean(WebClient.Builder.class);
            assertThat(context).hasSingleBean(RemoteHttpClient.class);
        });
    }
}
