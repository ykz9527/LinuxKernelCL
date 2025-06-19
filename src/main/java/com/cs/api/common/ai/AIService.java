package com.cs.api.common.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI模型调用服务
 * 提供简洁的静态方法调用接口，支持ChatGPT和DeepSeek
 */
@Service
public class AIService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private static AIService instance;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.chatgpt.api-key:}")
    private String chatgptApiKey;
    
    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;
    
    @Value("${ai.timeout:30}")
    private int timeoutSeconds;
    
    public AIService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
        instance = this;
    }
    
    /**
     * 使用ChatGPT生成回复（静态方法）
     * 
     * @param prompt 输入提示
     * @return AI生成的回复内容
     */
    public static String chatgpt(String prompt) {
        return getInstance().callAI(AIModelType.CHATGPT, prompt);
    }
    
    /**
     * 使用DeepSeek生成回复（静态方法）
     * 
     * @param prompt 输入提示
     * @return AI生成的回复内容
     */
    public static String deepseek(String prompt) {
        return getInstance().callAI(AIModelType.DEEPSEEK, prompt);
    }
    
    /**
     * 使用指定AI模型生成回复（静态方法）
     * 
     * @param modelType AI模型类型
     * @param prompt 输入提示
     * @return AI生成的回复内容
     */
    public static String call(AIModelType modelType, String prompt) {
        return getInstance().callAI(modelType, prompt);
    }
    
    /**
     * 异步调用AI模型（静态方法）
     * 
     * @param modelType AI模型类型
     * @param prompt 输入提示
     * @return CompletableFuture包装的回复内容
     */
    public static CompletableFuture<String> callAsync(AIModelType modelType, String prompt) {
        return CompletableFuture.supplyAsync(() -> getInstance().callAI(modelType, prompt));
    }
    
    /**
     * 获取服务实例
     */
    private static AIService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AIService not initialized. Make sure Spring context is loaded.");
        }
        return instance;
    }
    
    /**
     * 调用AI模型核心方法
     * 
     * @param modelType AI模型类型
     * @param prompt 输入提示
     * @return AI生成的回复内容
     */
    private String callAI(AIModelType modelType, String prompt) {
        try {
            logger.info("Calling AI model: {} with prompt length: {}", modelType.name(), prompt.length());
            
            // 验证API密钥
            String apiKey = getApiKey(modelType);
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalStateException("API key not configured for " + modelType.name());
            }
            
            // 构建请求
            AIRequest request = new AIRequest(
                modelType.getModelName(),
                List.of(Map.of("role", "user", "content", prompt))
            );
            
            // 发送请求
            String responseBody = webClient.post()
                .uri(modelType.getApiUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();
            
            // 解析响应
            AIResponse response = objectMapper.readValue(responseBody, AIResponse.class);
            String content = response.getContent();
            
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Empty response from AI model");
            }
            
            logger.info("AI model {} responded successfully", modelType.name());
            return content.trim();
            
        } catch (Exception e) {
            logger.error("Error calling AI model {}: {}", modelType.name(), e.getMessage(), e);
            throw new RuntimeException("AI模型调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取对应模型的API密钥
     */
    private String getApiKey(AIModelType modelType) {
        switch (modelType) {
            case CHATGPT:
                return chatgptApiKey;
            case DEEPSEEK:
                return deepseekApiKey;
            default:
                throw new IllegalArgumentException("Unsupported AI model type: " + modelType);
        }
    }
} 