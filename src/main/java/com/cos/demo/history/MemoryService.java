package com.cos.demo.history;

import com.cos.demo.constant.MemoryPromptConstant;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MemoryService {
    private static final int SUMMARY_TRIGGER = 20;
    private final OpenAIClient client;
    private final LongTermMemory longTermMemory ;
    private final MemoryStateManager stateManager;

    public MemoryService(LongTermMemory longTermMemory ,OpenAIClient client,MemoryStateManager stateManager) {
        this.longTermMemory  = longTermMemory ;
        this.client = client;
        this.stateManager = stateManager;
    }

    public boolean shouldSummary(String sessionId,List<ChatMessage> history) {
        MemoryState state = stateManager.getState(sessionId);
        int newMessages = history.size() - state.getSummarizedIndex();
        return newMessages >= SUMMARY_TRIGGER;
    }

    private String builderHistory(List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage chatMessage : history) {
            sb.append(chatMessage.getRole())
                    .append(":")
                    .append(chatMessage.getContent())
                    .append("\n");
        }
        return sb.toString();
    }

    public void summary(String sessionId,List<ChatMessage> history) {
        String historyText = builderHistory(history);

        // 通过模型取得摘要
        ChatCompletionCreateParams params =
                ChatCompletionCreateParams.builder()
                        .model("qwen3.7-plus")  // 摘要用更加便宜的模型
                        .addSystemMessage(MemoryPromptConstant.MEMORY_SUMMARY)
                        .addUserMessage(historyText)
                        .build();

        ChatCompletion completion =
                client.chat().completions().create(params);

        String summary = completion.choices()
                .getFirst()
                .message()
                .content()
                .orElse("");

        // 放进 LongTermMemory
        List<MemorySummary> summaries =
                longTermMemory.getMemory(sessionId);

        summaries.add(
                new MemorySummary(
                        summaries.size()+1,
                        summary
                )
        );
    }

    @Async
    public void summaryAsync(String sessionId,List<ChatMessage> history,int endIndex) {
        try {
            summary(sessionId,history);
            stateManager.getState(sessionId)
                    .setSummarizedIndex(endIndex);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
