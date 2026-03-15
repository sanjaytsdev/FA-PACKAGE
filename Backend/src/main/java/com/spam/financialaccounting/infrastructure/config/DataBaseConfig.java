package com.spam.financialaccounting.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataBaseConfig {

     @Bean
    public SQLAccess sqlAccess() throws ClassNotFoundException {
        return new SQLAccess(
                "org.sqlite.JDBC",
                "jdbc:sqlite:sqlite/FA.db"
        );
    }
}
