package com.cos.demo.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DioBO {
    private String sessionId;

    @NotBlank(message ="消息不能为空")
    private String msg;
}
