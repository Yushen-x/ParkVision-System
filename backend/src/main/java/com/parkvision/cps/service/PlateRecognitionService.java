package com.parkvision.cps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Real license-plate recognition through a pluggable provider.
 *
 * <p>Two providers are supported, selected via {@code parkvision.vision.lpr.provider}:
 * <ul>
 *   <li>{@code hyperlpr} — a local HyperLPR3 service (offline, free). The backend
 *       POSTs the image to {@code tools/hyperlpr/server.py}.</li>
 *   <li>{@code baidu} — Baidu OCR {@code license_plate} cloud endpoint. The API
 *       key/secret stay on the server, so the browser never talks to the
 *       provider directly (no CORS, no key leakage).</li>
 * </ul>
 *
 * <p>When no provider is configured (the default) {@link #recognize} returns an
 * empty result and the caller transparently falls back to the built-in engine,
 * so the system keeps working out-of-the-box without any external dependency.
 */
@Service
public class PlateRecognitionService {
    private static final Logger log = LoggerFactory.getLogger(PlateRecognitionService.class);
    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/license_plate";

    private final String provider;
    private final String hyperlprUrl;
    private final String apiKey;
    private final String secretKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private volatile String cachedToken;
    private volatile long tokenExpiresAt;

    public PlateRecognitionService(
            @Value("${parkvision.vision.lpr.provider:none}") String provider,
            @Value("${parkvision.vision.lpr.hyperlpr.url:http://localhost:8715/recognize}") String hyperlprUrl,
            @Value("${parkvision.vision.lpr.baidu.api-key:}") String apiKey,
            @Value("${parkvision.vision.lpr.baidu.secret-key:}") String secretKey,
            ObjectMapper objectMapper
    ) {
        this.provider = provider == null ? "none" : provider.trim().toLowerCase();
        this.hyperlprUrl = hyperlprUrl == null ? "" : hyperlprUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return switch (provider) {
            case "hyperlpr" -> !hyperlprUrl.isBlank();
            case "baidu" -> !apiKey.isBlank() && !secretKey.isBlank();
            default -> false;
        };
    }

    /**
     * Recognise the plate in an image. Returns empty when no provider is
     * configured or recognition fails, so callers can fall back gracefully.
     *
     * @param imageDataUrl a {@code data:image/...;base64,...} URL or a raw base64 string
     */
    public Optional<PlateReading> recognize(String imageDataUrl) {
        if (!isEnabled() || imageDataUrl == null || imageDataUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            return switch (provider) {
                case "hyperlpr" -> recognizeHyperlpr(imageDataUrl);
                case "baidu" -> recognizeBaidu(imageDataUrl);
                default -> Optional.empty();
            };
        } catch (Exception ex) {
            log.warn("License plate recognition failed, falling back to built-in engine: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<PlateReading> recognizeHyperlpr(String imageDataUrl) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("imageBase64", imageDataUrl));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(hyperlprUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(res.body());
        String plate = root.path("plate").asText("");
        if (plate.isBlank() || "null".equalsIgnoreCase(plate)) {
            return Optional.empty();
        }
        String color = root.path("color").asText("");
        double confidence = root.path("confidence").asDouble(0.96);
        return Optional.of(new PlateReading(plate.toUpperCase(), confidence, color, isNewEnergyColor(color)));
    }

    private Optional<PlateReading> recognizeBaidu(String imageDataUrl) throws Exception {
        String base64 = stripDataUrl(imageDataUrl);
        String token = accessToken();
        String body = "image=" + URLEncoder.encode(base64, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OCR_URL + "?access_token=" + token))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(res.body());
        if (root.has("error_code")) {
            log.warn("Baidu LPR error {}: {}", root.path("error_code").asInt(), root.path("error_msg").asText());
            return Optional.empty();
        }
        JsonNode result = root.path("words_result");
        if (result.isArray()) {
            if (result.isEmpty()) {
                return Optional.empty();
            }
            result = result.get(0);
        }
        String plate = result.path("number").asText("");
        if (plate.isBlank()) {
            return Optional.empty();
        }
        String color = result.path("color").asText("");
        double confidence = averageProbability(result.path("probability"));
        return Optional.of(new PlateReading(plate.toUpperCase(), confidence, color, isNewEnergyColor(color)));
    }

    private synchronized String accessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiresAt) {
            return cachedToken;
        }
        String url = TOKEN_URL + "?grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(secretKey, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(res.body());
        String token = root.path("access_token").asText("");
        if (token.isBlank()) {
            throw new IllegalStateException("Baidu token request failed: " + res.body());
        }
        long expiresInSec = root.path("expires_in").asLong(2_592_000L);
        cachedToken = token;
        // Refresh a minute early to avoid edge expiry.
        tokenExpiresAt = now + (expiresInSec - 60) * 1000L;
        return token;
    }

    private static String stripDataUrl(String imageDataUrl) {
        int comma = imageDataUrl.indexOf(',');
        if (imageDataUrl.startsWith("data:") && comma > 0) {
            return imageDataUrl.substring(comma + 1);
        }
        return imageDataUrl;
    }

    private static double averageProbability(JsonNode probability) {
        if (probability == null || !probability.isArray() || probability.isEmpty()) {
            return 0.96;
        }
        double sum = 0;
        int n = 0;
        for (JsonNode p : probability) {
            sum += p.asDouble(0);
            n++;
        }
        if (n == 0) {
            return 0.96;
        }
        return Math.round(sum / n * 1000.0) / 1000.0;
    }

    private static boolean isNewEnergyColor(String color) {
        return color != null && (color.contains("green") || color.contains("绿"));
    }

    public record PlateReading(String plate, double confidence, String color, boolean newEnergy) {
    }
}
