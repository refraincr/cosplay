package com.cos.demo.constant;

public class MemoryPromptConstant {
    public static final String MEMORY_SUMMARY = """
            你是一名长期记忆整理助手。
            
            请阅读下面的聊天记录，并生成一份长期记忆摘要。
            
            要求：
            
            1. 保留用户的重要信息
            2. 保留用户长期目标
            3. 保留用户偏好
            4. 保留已经完成的重要事情
            5. 删除寒暄
            6. 删除重复内容
            7. 删除无意义聊天
            8. 不要遗漏关键事实
            9. 输出纯文本
            10. 不要解释。
            
            聊天记录：
            """;
}
