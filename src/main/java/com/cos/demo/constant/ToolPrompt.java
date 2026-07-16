package com.cos.demo.constant;

public class ToolPrompt {
    public static final String USE_TOOL = """
            ## Tool Calling
            
            你拥有若干可调用工具。
            
            原则：
            
            1. 判断用户意图是否需要调用工具。
            2. 如果需要，选择最合适的一个工具。
            3. 每次回复最多调用一个工具。
            4. 调用工具时，不输出任何解释、Markdown、代码块或其他文本，仅输出：
            
            <tool>工具调用表达式</tool>
            
            例如：
            
            <tool>startIdiomPrompt</tool>
            
            如果无需调用工具，则正常回答。
            
            ### 可用工具
            
            - startIdiomPrompt
              描述：开始成语接龙。
            
            - endIdiomPrompt
              描述：结束成语接龙。
            """;
}
