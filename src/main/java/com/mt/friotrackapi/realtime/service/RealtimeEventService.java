package com.mt.friotrackapi.realtime.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class RealtimeEventService {
    private final Map<Long, List<SseEmitter>> emittersByCompany = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long companyId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByCompany.computeIfAbsent(companyId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(companyId, emitter));
        emitter.onTimeout(() -> remove(companyId, emitter));
        emitter.onError(error -> remove(companyId, emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("{}"));
        } catch (IOException ex) {
            remove(companyId, emitter);
        }
        return emitter;
    }

    public void publish(Long companyId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByCompany.getOrDefault(companyId, List.of());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException ex) {
                remove(companyId, emitter);
            }
        }
    }

    private void remove(Long companyId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByCompany.get(companyId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
