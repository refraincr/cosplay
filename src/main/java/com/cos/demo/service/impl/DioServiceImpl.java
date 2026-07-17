package com.cos.demo.service.impl;

import com.cos.demo.bo.DioBO;
import com.cos.demo.history.*;
import com.cos.demo.prompt.PromptBuilder;
import com.cos.demo.scene.SceneManager;
import com.cos.demo.service.RoleService;
import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@Service
public class DioServiceImpl implements RoleService {

    private final OpenAIClient client;
    private final ChatMemory chatMemory;
    private final MemoryService memoryService;
    private final MemoryStateManager stateManager;
    private final PromptBuilder promptBuilder;
    private final SceneManager sceneManager;

    public DioServiceImpl(OpenAIClient client,
                          ChatMemory chatMemory,
                          MemoryService memoryService,
                          MemoryStateManager stateManager,
                          PromptBuilder promptBuilder,
                          SceneManager sceneManager) {
        this.client = client;
        this.chatMemory = chatMemory;
        this.memoryService = memoryService;
        this.stateManager = stateManager;
        this.promptBuilder = promptBuilder;
        this.sceneManager = sceneManager;
    }

    @Override
    public void talk(DioBO dioBO, SseEmitter emitter) {
        ChatCompletionCreateParams.Builder builder =
                ChatCompletionCreateParams.builder().model("qwen3.7-max");

        List<ChatMessage> history = chatMemory.getHistory(dioBO.getSessionId());
        // 添加系统消息,最近消息,可能的摘要,当前消息
        promptBuilder.build(builder,dioBO,history);

        ChatCompletionCreateParams params = builder.build();
        try (StreamResponse<ChatCompletionChunk> stream =
                     client.chat().completions().createStreaming(params)) {

            // 拼接Assistant信息
            StringBuilder answer = new StringBuilder();
            stream.stream().forEach(chunk -> {
                if (chunk.choices().isEmpty()) return;
                ChatCompletionChunk.Choice choice = chunk.choices().getFirst();
                ChatCompletionChunk.Choice.Delta delta = choice.delta();
                // 普通文本
                delta.content().ifPresent(answer::append);
            });

            String finalAnswer = answer.toString();

            //保存user,Assistant信息
            addUserAndAssistant(dioBO.getMsg(), finalAnswer, history);

            // 若要做摘要
            if (memoryService.shouldSummary(dioBO.getSessionId(),history)) addSummary(dioBO.getSessionId(),history);

            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    @Override
    public String talkNoStream(DioBO dioBO) {
        String answer;
        String assistantRes = "";
        while (true) {
            // 构造参数
            ChatCompletionCreateParams.Builder builder =
                    ChatCompletionCreateParams.builder().model("qwen3.7-max");

            // 添加系统消息,最近消息,可能的摘要,当前消息
            List<ChatMessage> history = chatMemory.getHistory(dioBO.getSessionId());
            promptBuilder.build(builder,dioBO,history);

            if (!assistantRes.isEmpty()) {
                builder.addAssistantMessage(assistantRes);
            }

            ChatCompletionCreateParams params = builder.build();

            // 模型回复
            ChatCompletionMessage message = client.chat()
                    .completions()
                    .create(params)
                    .choices()
                    .getFirst()
                    .message();

            // 是否使用工具
            List<ChatCompletionMessageToolCall> toolCalls =
                    message.toolCalls().orElse(Collections.emptyList());
            if (!toolCalls.isEmpty()) {
                for (ChatCompletionMessageToolCall toolCall : toolCalls) {
                    String name = toolCall.function()
                            .orElseThrow()
                            .function()
                            .name();
                    // 暂时调用，展示可行性
                    if ("StartIdiom".equals(name)) {
                        addPromptAndResponse();
                        assistantRes = "已经开始接龙";
                    }
                }
                continue;
            }

            answer = message.content().orElse("");

            // 消息加入历史
            addUserAndAssistant(dioBO.getMsg(), answer, history);

            // 是否摘要
            if (memoryService.shouldSummary(dioBO.getSessionId(),history)) addSummary(dioBO.getSessionId(),history);
            break;
        }


        return answer;
    }

    private void addPromptAndResponse() {
        sceneManager.changeScene("IDIOM");
    }

    private void addUserAndAssistant(String userMsg, String assistantMsg,List<ChatMessage> history) {
        history.add(new ChatMessage("user", userMsg));
        history.add(new ChatMessage("assistant", assistantMsg));
    }

    private void addSummary(String sessionId, List<ChatMessage> history) {
        // 防止重复摘要，只摘要最新的
        MemoryState state = stateManager.getState(sessionId);
        List<ChatMessage> newHistory =
                history.subList(
                        state.getSummarizedIndex(),
                        history.size()
                );
        // 异步获取摘要
        memoryService.summaryAsync(
                sessionId,
                new ArrayList<>(newHistory),
                history.size()
        );
        // 更新state
        state.setSummarizedIndex(history.size());
    }
}
