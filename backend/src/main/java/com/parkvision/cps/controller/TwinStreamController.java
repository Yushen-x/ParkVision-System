package com.parkvision.cps.controller;

import com.parkvision.cps.service.TwinBroadcastService;
import com.parkvision.cps.web.TwinStreamHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Real-time digital-twin channel. Clients subscribe with the browser EventSource
 * API and receive authoritative state snapshots pushed by {@link TwinBroadcastService}.
 */
@RestController
@RequestMapping("/api/twin")
public class TwinStreamController {

    private final TwinStreamHub hub;
    private final TwinBroadcastService broadcastService;

    public TwinStreamController(TwinStreamHub hub, TwinBroadcastService broadcastService) {
        this.hub = hub;
        this.broadcastService = broadcastService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = hub.register();
        // Push the current state immediately so the client renders without waiting a tick.
        String snapshot = broadcastService.currentSnapshotJson();
        if (snapshot != null) {
            hub.send(emitter, snapshot);
        }
        return emitter;
    }
}
