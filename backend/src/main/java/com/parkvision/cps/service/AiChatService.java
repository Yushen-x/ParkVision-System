package com.parkvision.cps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.parkvision.cps.common.BusinessException;
import com.parkvision.cps.dto.ai.ChatMessage;
import com.parkvision.cps.dto.ai.ChatReply;
import com.parkvision.cps.dto.ai.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Server-side proxy to an OpenAI-compatible chat model (DeepSeek by default).
 *
 * <p>Keeping the call on the server means the API key never reaches the browser
 * and there is no CORS problem (DeepSeek does not allow direct browser calls).
 * When no key is configured the service reports itself disabled and the frontend
 * transparently falls back to its built-in rule-based assistant.
 */
@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public AiChatService(
            @Value("${parkvision.ai.chat.provider:none}") String provider,
            @Value("${parkvision.ai.chat.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${parkvision.ai.chat.api-key:}") String apiKey,
            @Value("${parkvision.ai.chat.model:deepseek-chat}") String model,
            @Value("${parkvision.ai.chat.temperature:0.4}") double temperature,
            ObjectMapper objectMapper
    ) {
        this.provider = provider == null ? "none" : provider.trim().toLowerCase();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "deepseek-chat" : model.trim();
        this.temperature = temperature;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return !"none".equals(provider) && !apiKey.isBlank() && !baseUrl.isBlank();
    }

    public String model() {
        return model;
    }

    public ChatReply chat(ChatRequest request) {
        if (!isEnabled()) {
            throw new BusinessException("AI_DISABLED", "未配置对话大模型");
        }
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            throw new BusinessException("AI_BAD_REQUEST", "缺少对话内容");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", request.temperature() != null ? request.temperature() : temperature);
            body.put("max_tokens", 1024);
            body.put("stream", false);

            ArrayNode messages = body.putArray("messages");
            if (request.system() != null && !request.system().isBlank()) {
                ObjectNode sys = messages.addObject();
                sys.put("role", "system");
                sys.put("content", request.system());
            }
            for (ChatMessage m : request.messages()) {
                if (m == null || m.content() == null) {
                    continue;
                }
                ObjectNode node = messages.addObject();
                node.put("role", "assistant".equals(m.role()) ? "assistant" : "user");
                node.put("content", m.content());
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                log.warn("Chat model returned {}: {}", response.statusCode(), response.body());
                throw new BusinessException("AI_UPSTREAM", "对话模型返回 " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            if (text.isBlank()) {
                throw new BusinessException("AI_EMPTY", "对话模型未返回内容");
            }
            return new ChatReply(text.trim(), model);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Chat model call failed: {}", ex.getMessage());
            throw new BusinessException("AI_ERROR", "对话模型调用失败");
        }
    }
}
