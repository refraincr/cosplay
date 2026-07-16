package com.cos.demo.service.impl;

import com.cos.demo.bo.DioBO;
import com.cos.demo.history.*;
import com.cos.demo.service.RoleService;
import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cos.demo.constant.PromptConstant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DioServiceImpl implements RoleService {

    private final OpenAIClient client;
    private final ChatMemory chatMemory;
    private final MemoryService memoryService;
    private final MemoryStateManager stateManager;
    private final LongTermMemory  longTermMemory;

    public DioServiceImpl(OpenAIClient client,ChatMemory chatMemory,MemoryService memoryService,MemoryStateManager stateManager,LongTermMemory  longTermMemory) {
        this.client = client;
        this.chatMemory = chatMemory;
        this.memoryService = memoryService;
        this.stateManager = stateManager;
        this.longTermMemory = longTermMemory;
    }

    @Override
    public void talk(DioBO dioBO, SseEmitter emitter) {

        ChatCompletionCreateParams.Builder builder =
                ChatCompletionCreateParams.builder().model("qwen3.7-max");

        // 加入系统消息
        builder.addSystemMessage(PromptConstant.DIO);

        // 添加过去的聊天摘要
        String memory = longTermMemory.buildMemoryPrompt(dioBO.getSessionId());
        if (!memory.isBlank()) {
            builder.addSystemMessage(memory);
        }

        // 加入历史的消息（获取最近的20条消息）
        List<ChatMessage> history =
                chatMemory.getHistory(dioBO.getSessionId());
        int start = Math.max(0,history.size()-20);
//        List<ChatMessage> recent = history.subList(start, history.size());
        List<ChatMessage> recent = new ArrayList<>(history.subList(start,history.size()));
        for (ChatMessage msg: recent) {
            if ("user".equals(msg.getRole())) {
                builder.addUserMessage(msg.getContent());
            } else {
                builder.addAssistantMessage(msg.getContent());
            }
        }

        // 加入当前消息
        builder.addUserMessage(dioBO.getMsg());

        // 保存用户信息
        history.add(
                new ChatMessage(
                        "user",
                        dioBO.getMsg()
                )
        );

        // 拼接Assistant信息
        StringBuilder answer =  new StringBuilder();
        ChatCompletionCreateParams params = builder.build();
        try (StreamResponse<ChatCompletionChunk> stream =
                client.chat().completions().createStreaming(params)) {
            stream.stream().forEach(chunk->{
                if (!chunk.choices().isEmpty()) {
                    String content = chunk.choices()
                            .getFirst()
                            .delta()
                            .content()
                            .orElse("");
                    if (!content.isEmpty()) {
                        try {
                            answer.append(content);
                            emitter.send(content);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            //保存Assistant信息
            history.add(
                    new ChatMessage(
                            "assistant",
                            answer.toString()
                    )
            );

            // 若要做摘要
            if (memoryService.shouldSummary(dioBO.getSessionId(),history)) {
                // 防止重复摘要，只摘要最新的
                MemoryState state = stateManager.getState(dioBO.getSessionId());
                List<ChatMessage> newHistory =
                        history.subList(
                                state.getSummarizedIndex(),
                                history.size()
                        );
                // 异步获取摘要
                memoryService.summaryAsync(
                        dioBO.getSessionId(),
                        new ArrayList<>(newHistory),
                        history.size()
                );

                // 更新state
                state.setSummarizedIndex(history.size());
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
