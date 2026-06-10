package com.parkvision.cps.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks the live Server-Sent Events subscribers for the digital twin and fans
 * out authoritative state snapshots to all of them. SSE is a one-directional
 * server-to-client push, which is exactly what the read-only twin stream needs;
 * the broadcaster ({@code TwinBroadcastService}) is the single writer.
 */
@Component
public class TwinStreamHub {

    // Never time out on the server side; the browser EventSource handles reconnects.
    private static final long NO_TIMEOUT = 0L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError(ex -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    public boolean hasClients() {
        return !emitters.isEmpty();
    }

    public void send(SseEmitter emitter, String payload) {
        try {
            emitter.send(SseEmitter.event().name("twin").data(payload));
        } catch (IOException | IllegalStateException ex) {
            emitter.complete();
            emitters.remove(emitter);
        }
    }

    public void broadcast(String payload) {
        for (SseEmitter emitter : emitters) {
            send(emitter, payload);
        }
    }
}
