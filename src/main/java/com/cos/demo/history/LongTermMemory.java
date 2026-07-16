package com.cos.demo.history;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LongTermMemory {
    private final ConcurrentHashMap<String, List<MemorySummary>> memory =
            new ConcurrentHashMap<>();

    public List<MemorySummary> getMemory(String sessionId) {
        return memory.computeIfAbsent(
                sessionId,
                k->new ArrayList<>()
        );
    }

    /*
      构建 buildMemoryPrompt
      @param String sessionId 回话id
      @return String 构造的MemoryPrompt结果
      */
    public String buildMemoryPrompt(String sessionId) {
        List<MemorySummary> summaries = getMemory(sessionId);

        if (summaries.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        sb.append("""
                以下是对之前聊天内容的长期记忆摘要，
                回答时可以参考，
                若与当前聊天冲突，以当前聊天为准。
                """);

        for  (MemorySummary summary : summaries) {
            sb.append("【摘要")
                    .append(summary.getIndex())
                    .append("】\n");

            sb.append(summary.getContent())
                    .append("\n\n");
        }

        return sb.toString();
    }
}