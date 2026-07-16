package com.cos.demo.history;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryStateManager {
    private final ConcurrentHashMap<String, MemoryState> states =
            new ConcurrentHashMap<>();
    public MemoryState getState(String sessionId) {
        return states.computeIfAbsent(
                sessionId,
                k->new MemoryState(0)
        );
    }
}
