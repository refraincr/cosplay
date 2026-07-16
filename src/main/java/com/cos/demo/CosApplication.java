package com.cos.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class CosApplication {

    public static void main(String[] args) {
        SpringApplication.run(CosApplication.class, args);
    }

}
