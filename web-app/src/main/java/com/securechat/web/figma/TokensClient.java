package com.securechat.web.figma;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class TokensClient {
    private static final Logger log = LoggerFactory.getLogger(TokensClient.class);

    private final RestTemplate restTemplate;
    private final String url;
    private final boolean enabled;

    public TokensClient(RestTemplate restTemplate, String url, boolean enabled) {
        this.restTemplate = restTemplate;
        this.url = url;
        this.enabled = enabled;
    }

    public Optional<Map<String, Object>> fetchTokens() {
        if (!enabled) {
            log.info("Design token refresh disabled by configuration");
            return Optional.empty();
        }
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Fetched design tokens from MCP server");
                return Optional.of(response.getBody());
            }
            log.warn("Unexpected response from MCP server: {}", response.getStatusCode());
        } catch (RestClientException ex) {
            log.warn("Failed to fetch design tokens from MCP server", ex);
        }
        return Optional.empty();
    }
}
