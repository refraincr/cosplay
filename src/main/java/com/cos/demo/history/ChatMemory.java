package com.cos.demo.history;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatMemory {
    private final ConcurrentHashMap<String, List<ChatMessage>> memory =
            new ConcurrentHashMap<>();

    public List<ChatMessage> getHistory(String sessionId) {
        return memory.computeIfAbsent(
                sessionId,
                k -> new ArrayList<>()
        );
    }
}
