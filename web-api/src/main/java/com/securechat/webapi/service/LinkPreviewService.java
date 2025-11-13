package com.securechat.webapi.service;

import com.securechat.webapi.entity.LinkPreviewEntity;
import com.securechat.webapi.repository.LinkPreviewRepository;
import com.securechat.webapi.telemetry.LinkPreviewEvent;
import com.securechat.webapi.telemetry.LinkPreviewEventBus;
import com.securechat.webapi.telemetry.NetworkServiceKeys;
import com.securechat.webapi.telemetry.NetworkServiceRegistry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class LinkPreviewService {
    private static final Logger log = LoggerFactory.getLogger(LinkPreviewService.class);
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;
    private static final int MAX_CONTENT_LENGTH = 1024 * 1024; // 1MB
    private static final Pattern LOCAL_NETWORK_PATTERN = Pattern.compile(
            "^(127\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|localhost|::1)"
    );

    private final LinkPreviewRepository linkPreviewRepository;
    private final NetworkServiceRegistry networkServiceRegistry;
    private final LinkPreviewEventBus eventBus;

    @Autowired
    public LinkPreviewService(LinkPreviewRepository linkPreviewRepository,
                              NetworkServiceRegistry networkServiceRegistry,
                              LinkPreviewEventBus eventBus) {
        this.linkPreviewRepository = linkPreviewRepository;
        this.networkServiceRegistry = networkServiceRegistry;
        this.eventBus = eventBus;
        networkServiceRegistry.registerService(
            NetworkServiceKeys.LINK_PREVIEW,
            "URI Safety & Preview",
            "HTTP/S",
            "outbound",
            "Validates chat links and fetches safe metadata"
        );
        networkServiceRegistry.serviceRunning(
            NetworkServiceKeys.LINK_PREVIEW,
            "Ready to inspect outbound links from chat messages"
        );
    }

    public LinkPreviewEntity getOrFetchPreview(String urlString) {
        // Check cache first
        Optional<LinkPreviewEntity> cached = linkPreviewRepository.findByUrl(urlString);
        if (cached.isPresent() && cached.get().getExpiresAt() != null 
            && cached.get().getExpiresAt().isAfter(Instant.now())) {
            log.info("🔗 LINK PREVIEW (CACHED): {} → '{}'", urlString, 
                cached.get().getTitle() != null ? cached.get().getTitle() : "No title");
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.LINK_PREVIEW,
                "CACHE",
                "Served cached preview for " + urlString
            );
            eventBus.publish(LinkPreviewEvent.of(urlString,
                    cached.get().getTitle(),
                    "CACHE",
                    "Served cached preview"));
            return cached.get();
        }

        // Validate URL and check for SSRF
        if (!isValidUrl(urlString)) {
            log.warn("🔗 LINK PREVIEW (INVALID URL): {}", urlString);
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.LINK_PREVIEW,
                "REJECT",
                "Invalid URL blocked: " + urlString
            );
            eventBus.publish(LinkPreviewEvent.of(urlString, null, "REJECT", "Invalid URL"));
            throw new IllegalArgumentException("Invalid URL: " + urlString);
        }

        if (isLocalNetwork(urlString)) {
            log.warn("🔗 LINK PREVIEW (SSRF BLOCKED): {} - Local network access denied", urlString);
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.LINK_PREVIEW,
                "REJECT",
                "Local/SSRF URL blocked: " + urlString
            );
            eventBus.publish(LinkPreviewEvent.of(urlString, null, "REJECT", "Local network blocked"));
            throw new SecurityException("Access to local network is not allowed");
        }

        try {
            log.info("🔗 LINK PREVIEW (FETCHING): {}", urlString);
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.LINK_PREVIEW,
                "FETCH",
                "Fetching metadata for " + urlString
            );
            LinkPreviewEntity preview = fetchPreview(urlString);
            LinkPreviewEntity saved = linkPreviewRepository.save(preview);
            String title = saved.getTitle() != null ? saved.getTitle() : "No title";
            log.info("🔗 LINK PREVIEW (FETCHED): {} → '{}'", urlString, title);
            networkServiceRegistry.recordUsage(
                NetworkServiceKeys.LINK_PREVIEW,
                "STORE",
                "Stored preview for " + urlString
            );
            eventBus.publish(LinkPreviewEvent.of(urlString, title, "FETCH", "Fetched metadata"));
            return saved;
        } catch (IOException e) {
            log.error("🔗 LINK PREVIEW (FAILED): {} - {}", urlString, e.getMessage());
            networkServiceRegistry.serviceDegraded(
                NetworkServiceKeys.LINK_PREVIEW,
                "Failed to fetch " + urlString + ": " + e.getMessage()
            );
            eventBus.publish(LinkPreviewEvent.of(urlString, null, "ERROR", e.getMessage()));
            throw new RuntimeException("Failed to fetch preview: " + e.getMessage(), e);
        }
    }

    private boolean isValidUrl(String urlString) {
        try {
            new URL(urlString);
            return urlString.startsWith("http://") || urlString.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLocalNetwork(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            return LOCAL_NETWORK_PATTERN.matcher(host).find();
        } catch (Exception e) {
            return true; // Err on the side of caution
        }
    }

    private LinkPreviewEntity fetchPreview(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        log.debug("🔗 LINK PREVIEW: HTTP {} for {}", responseCode, urlString);

        // Check content type
        String contentType = connection.getContentType();
        if (contentType == null || !contentType.startsWith("text/html")) {
            log.warn("🔗 LINK PREVIEW (UNSUPPORTED TYPE): {} - Content-Type: {}", urlString, contentType);
            throw new IOException("Unsupported content type: " + contentType);
        }

        // Check content length
        int contentLength = connection.getContentLength();
        if (contentLength > MAX_CONTENT_LENGTH) {
            log.warn("🔗 LINK PREVIEW (TOO LARGE): {} - Size: {} bytes", urlString, contentLength);
            throw new IOException("Content too large: " + contentLength);
        }

        // Parse HTML
        Document doc = Jsoup.parse(connection.getInputStream(), "UTF-8", urlString);
        doc.setBaseUri(urlString);

        LinkPreviewEntity preview = new LinkPreviewEntity();
        preview.setUrl(urlString);

        // Extract title
        Element titleElement = doc.selectFirst("meta[property=og:title]");
        if (titleElement == null) {
            titleElement = doc.selectFirst("title");
        }
        if (titleElement != null) {
            preview.setTitle(titleElement.attr("content").isEmpty() 
                ? titleElement.text() 
                : titleElement.attr("content"));
        }

        // Extract description
        Element descElement = doc.selectFirst("meta[property=og:description]");
        if (descElement == null) {
            descElement = doc.selectFirst("meta[name=description]");
        }
        if (descElement != null) {
            preview.setDescription(descElement.attr("content"));
        }

        // Extract icon/favicon
        Element iconElement = doc.selectFirst("link[rel=icon]");
        if (iconElement == null) {
            iconElement = doc.selectFirst("link[rel=shortcut icon]");
        }
        if (iconElement == null) {
            iconElement = doc.selectFirst("meta[property=og:image]");
        }
        if (iconElement != null) {
            String iconUrl = iconElement.attr("href");
            if (!iconUrl.isEmpty()) {
                if (iconUrl.startsWith("//")) {
                    iconUrl = url.getProtocol() + ":" + iconUrl;
                } else if (iconUrl.startsWith("/")) {
                    iconUrl = url.getProtocol() + "://" + url.getHost() + iconUrl;
                }
                preview.setIconUrl(iconUrl);
            }
        }

        return preview;
    }
}




