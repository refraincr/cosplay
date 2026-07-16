package com.cos.demo.service.impl;

import com.cos.demo.bo.DioBO;
import com.cos.demo.history.*;
import com.cos.demo.prompt.PromptBuilder;
import com.cos.demo.service.RoleService;
import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DioServiceImpl implements RoleService {

    private final OpenAIClient client;
    private final ChatMemory chatMemory;
    private final MemoryService memoryService;
    private final MemoryStateManager stateManager;
    private final PromptBuilder promptBuilder;

    public DioServiceImpl(OpenAIClient client,
                          ChatMemory chatMemory,
                          MemoryService memoryService,
                          MemoryStateManager stateManager,
                          PromptBuilder promptBuilder) {
        this.client = client;
        this.chatMemory = chatMemory;
        this.memoryService = memoryService;
        this.stateManager = stateManager;
        this.promptBuilder = promptBuilder;
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

            String finalAnswer = answer.toString();

            //保存user,Assistant信息
            history.add(new ChatMessage("user", dioBO.getMsg()));
            history.add(new ChatMessage("assistant", finalAnswer));

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

            // 判断并提取工具符号
            if (finalAnswer.contains("<tool>") && finalAnswer.contains("</tool>")) {
                Pattern pattern = Pattern.compile("<tool>(.*?)</tool>",Pattern.DOTALL);
                Matcher matcher = pattern.matcher(finalAnswer);

                while (matcher.find()) {
                    String toolCallContent = matcher.group(1);
                    System.out.println(toolCallContent+"<=======");
                }
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
