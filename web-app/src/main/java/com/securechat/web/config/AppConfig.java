package com.securechat.web.config;

import com.securechat.web.figma.TokensClient;
import com.securechat.web.figma.TokensConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    RestTemplate figmaRestTemplate(RestTemplateBuilder builder,
                                   @Value("${app.tokens.key:dev-key}") String apiKey) {
        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            if (apiKey != null && !apiKey.isBlank()) {
                request.getHeaders().setBearerAuth(apiKey);
            }
            return execution.execute(request, body);
        };
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .additionalInterceptors(authInterceptor)
                .build();
    }

    @Bean
    TokensClient tokensClient(RestTemplate restTemplate,
                              @Value("${app.tokens.url:http://localhost:8765/tokens}") String url,
                              @Value("${app.tokens.enabled:true}") boolean enabled) {
        return new TokensClient(restTemplate, url, enabled);
    }

    @Bean
    TokensConverter tokensConverter() {
        return new TokensConverter();
    }

    @Bean
    ScheduledExecutorService sseKeepAliveScheduler() {
        return Executors.newScheduledThreadPool(1);
    }

    @Bean
    CommandLineRunner tokensCommandLineRunner(TokensClient client,
                                              TokensConverter converter,
                                              @Value("${app.tokens.output:src/main/resources/static/css/tokens.css}") String outputPath) {
        Path target = Path.of(outputPath);
        return args -> {
            try {
                Files.createDirectories(target.getParent());
            } catch (Exception e) {
                log.warn("Unable to ensure tokens directory exists: {}", target.getParent(), e);
            }
            client.fetchTokens().ifPresent(tokens -> {
                try {
                    String css = converter.toCss(tokens);
                    Files.writeString(target, css);
                    log.info("Updated design tokens at {}", target.toAbsolutePath());
                } catch (Exception e) {
                    log.warn("Failed to write tokens.css, keeping previous version", e);
                }
            });
        };
    }
}
