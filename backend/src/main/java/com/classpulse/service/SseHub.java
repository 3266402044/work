package com.classpulse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseHub {
    private final ObjectMapper objectMapper;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        sendRaw(emitter, "connected", Map.of("userId", userId));
        return emitter;
    }

    public void send(Long userId, String event, Object data) {
        if (userId == null) {
            return;
        }
        var list = emitters.get(userId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            sendRaw(emitter, event, data);
        }
    }

    public void broadcast(String event, Object data) {
        emitters.values().forEach(list -> list.forEach(emitter -> sendRaw(emitter, event, data)));
    }

    private void sendRaw(SseEmitter emitter, String event, Object data) {
        try {
            String json = data == null ? "{}" : objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(event).data(json));
        } catch (Exception ex) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        var list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}
