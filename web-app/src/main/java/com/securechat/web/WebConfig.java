package com.securechat.web;

import com.securechat.core.BroadcastHub;
import com.securechat.core.UserRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public BroadcastHub broadcastHub() {
        return new BroadcastHub();
    }

    @Bean
    public UserRegistry userRegistry() {
        return new UserRegistry();
    }
}
