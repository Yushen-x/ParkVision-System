package com.parkvision.cps.dto.ai;

import java.util.List;

/** Owner-assistant chat request proxied to the configured LLM. */
public record ChatRequest(String system, List<ChatMessage> messages, Double temperature) {
}
