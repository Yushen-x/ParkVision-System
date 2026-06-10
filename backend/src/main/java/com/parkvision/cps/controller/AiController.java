package com.parkvision.cps.controller;

import com.parkvision.cps.common.ApiResponse;
import com.parkvision.cps.dto.ai.ChatReply;
import com.parkvision.cps.dto.ai.ChatRequest;
import com.parkvision.cps.service.AiChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiChatService aiChatService;

    public AiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of(
                "live", aiChatService.isEnabled(),
                "model", aiChatService.model()
        ));
    }

    @PostMapping("/chat")
    public ApiResponse<ChatReply> chat(@RequestBody ChatRequest request) {
        return ApiResponse.ok(aiChatService.chat(request));
    }
}
