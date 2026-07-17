package com.cos.demo.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonClassDescription("当用户想要玩成语接龙游戏时，调用此工具")
public class StartIdiom {
    @JsonPropertyDescription("我们开始成语接龙吧")
    public String game;
}
