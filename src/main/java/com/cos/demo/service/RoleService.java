package com.cos.demo.service;

import com.cos.demo.bo.DioBO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface RoleService {
    void talk(DioBO dioBO, SseEmitter emitter);
}
