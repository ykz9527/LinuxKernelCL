package com.cs.api.common.ai;

/**
 * AI模型类型枚举
 * 定义支持的大模型类型和相关配置
 */
public enum AIModelType {
    /**
     * OpenAI ChatGPT
     */
    CHATGPT("gpt-4o-mini", "https://api.gptsapi.net/v1/chat/completions"),
    
    /**
     * DeepSeek
     */
    DEEPSEEK("deepseek-chat", "https://api.deepseek.com/v1/chat/completions");
    
    private final String modelName;
    private final String apiUrl;
    
    AIModelType(String modelName, String apiUrl) {
        this.modelName = modelName;
        this.apiUrl = apiUrl;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
} 