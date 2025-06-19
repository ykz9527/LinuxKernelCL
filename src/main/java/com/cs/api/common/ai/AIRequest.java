package com.cs.api.common.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * AI模型请求数据传输对象
 */
public class AIRequest {
    
    private String model;
    private List<Map<String, String>> messages;
    private Double temperature;
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    public AIRequest() {}
    
    public AIRequest(String model, List<Map<String, String>> messages) {
        this.model = model;
        this.messages = messages;
        this.temperature = 0.7;
        this.maxTokens = 1000;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Map<String, String>> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Map<String, String>> messages) {
        this.messages = messages;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}