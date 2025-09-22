package com.example.ragone.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j配置类
 */
@Configuration
public class LangChainConfig {
    
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;
    
    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;
    
    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String chatModelName;
    
    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private Double temperature;
    
    @Value("${langchain4j.open-ai.chat-model.max-tokens:2000}")
    private Integer maxTokens;
    
    @Value("${langchain4j.open-ai.chat-model.timeout:60s}")
    private String timeout;
    
    @Value("${langchain4j.open-ai.embedding-model.model-name}")
    private String embeddingModelName;
    
    /**
     * 配置聊天语言模型
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.parse("PT" + timeout.replace("s", "S")))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
    
    /**
     * 配置嵌入模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.parse("PT" + timeout.replace("s", "S")))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
