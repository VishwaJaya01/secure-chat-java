package com.securechat.webapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SecureCollabApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureCollabApplication.class, args);
    }
}

