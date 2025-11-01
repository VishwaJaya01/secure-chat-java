package com.securechat.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("db")
public class DbConfig {
    private static final Logger log = LoggerFactory.getLogger(DbConfig.class);

    @Bean
    CommandLineRunner databaseLogger(DataSource dataSource) {
        return args -> log.info("SecureChat DB profile active using datasource {}", dataSource);
    }
}
