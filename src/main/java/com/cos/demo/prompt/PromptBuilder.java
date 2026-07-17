package com.cos.demo.prompt;

import com.cos.demo.bo.DioBO;
import com.cos.demo.constant.PromptConstant;
import com.cos.demo.constant.ScenePromptConstant;
import com.cos.demo.history.ChatMessage;
import com.cos.demo.history.LongTermMemory;
import com.cos.demo.scene.Scene;
import com.cos.demo.scene.SceneState;
import com.cos.demo.tool.StartIdiom;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptBuilder {

    private final LongTermMemory longTermMemory;

    public PromptBuilder(LongTermMemory longTermMemory) {
        this.longTermMemory = longTermMemory;
    }

    public void build(ChatCompletionCreateParams.Builder builder,
                      DioBO dioBO,
                      List<ChatMessage> history) {
        // 添加历史消息(可能的摘要)
        addSystemMessage(builder,dioBO);

        // 添加最近 num 条消息
        int num = 20;
        addCurrentMessage(builder,history,num);

        // 加入当前消息
        builder.addUserMessage(dioBO.getMsg());

        // 添加工具
        builder.addTool(StartIdiom.class);
    }

    private void addSystemMessage(ChatCompletionCreateParams.Builder builder,
                                  DioBO dioBO) {
        builder.addSystemMessage(PromptConstant.DIO);

        // 添加场景prompt
        if (SceneState.scene == Scene.IDIOM) {
            builder.addSystemMessage(ScenePromptConstant.IDIOM);
        }

        // 添加过去的聊天摘要
        String memory = longTermMemory.buildMemoryPrompt(dioBO.getSessionId());
        if (!memory.isBlank()) {
            builder.addSystemMessage(memory);
        }
    }

    private void addCurrentMessage(ChatCompletionCreateParams.Builder builder,
                                   List<ChatMessage> history,
                                   int num) {
        // 加入历史的消息（获取最近的20条消息）
        int start = Math.max(0,history.size()-num);
        // subList的到的是浅拷贝后续的修改（history.add）会影响当前recent，所以要使用新的ArrayList
        List<ChatMessage> recent = new ArrayList<>(history.subList(start,history.size()));
        for (ChatMessage msg: recent) {
            if ("user".equals(msg.getRole())) {
                builder.addUserMessage(msg.getContent());
            } else {
                builder.addAssistantMessage(msg.getContent());
            }
        }
    }
}
