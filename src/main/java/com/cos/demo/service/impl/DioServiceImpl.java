package com.cos.demo.service.impl;

import com.cos.demo.bo.DioBO;
import com.cos.demo.history.ChatMemory;
import com.cos.demo.history.ChatMessage;
import com.cos.demo.service.RoleService;
import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cos.demo.constant.PromptConstant;

import java.io.IOException;
import java.util.List;

@Service
public class DioServiceImpl implements RoleService {

    private final OpenAIClient client;
    private final ChatMemory chatMemory;

    public DioServiceImpl(OpenAIClient client,ChatMemory chatMemory) {
        this.client = client;
        this.chatMemory = chatMemory;
    }

    @Override
    public void talk(DioBO dioBO, SseEmitter emitter) {

        ChatCompletionCreateParams.Builder builder =
                ChatCompletionCreateParams.builder().model("qwen3.7-max");

        // 加入系统消息
        builder.addSystemMessage(PromptConstant.DIO);

        // 加入历史的消息
        List<ChatMessage> history =
                chatMemory.getHistory(dioBO.getSessionId());
        for (ChatMessage msg: history) {
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
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
