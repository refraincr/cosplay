package com.cos.demo.controller;

import com.cos.demo.bo.DioBO;
import com.cos.demo.service.impl.DioServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("test")
public class CosController {
    private final DioServiceImpl dioService;

    public CosController(DioServiceImpl dioService) {
        this.dioService = dioService;
    }

    @GetMapping(value = "dio",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter dio(@Valid @RequestParam String msg,String sessionId){
        SseEmitter emitter = new SseEmitter(0L);

        DioBO dioBO = new DioBO();
        dioBO.setMsg(msg);
        dioBO.setSessionId(sessionId);
        // 异步处理
        CompletableFuture.runAsync(() -> dioService.talk(dioBO,emitter));

        return emitter;
    }
}
