package com.parkvision.cps.dto.ai;

/** A single chat turn. role is "user" or "assistant". */
public record ChatMessage(String role, String content) {
}
