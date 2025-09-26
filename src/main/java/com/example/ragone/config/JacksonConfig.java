package com.example.ragone.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson配置类 - 处理Hibernate代理对象序列化和Java 8时间类型
 */
@Configuration
public class JacksonConfig {
    
    /**
     * 配置ObjectMapper以正确处理Hibernate代理对象和Java 8时间类型
     * 使用Spring的Jackson2ObjectMapperBuilder来保持默认配置
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                // 注册Java 8时间模块
                .modules(new JavaTimeModule())
                // 禁用FAIL_ON_EMPTY_BEANS，避免空对象序列化错误
                .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                // 禁用自引用检测，避免循环引用错误
                .featuresToDisable(SerializationFeature.FAIL_ON_SELF_REFERENCES)
                // 禁用写入日期为时间戳，使用ISO-8601格式
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
