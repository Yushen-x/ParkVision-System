package com.parkvision.cps.dto.ai;

/** LLM reply returned to the browser. */
public record ChatReply(String text, String model) {
}
