package com.cs.api.common.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * AI模型响应数据传输对象 - 简化版本
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIResponse {
    
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Integer index;
        private Map<String, String> message;
        @JsonProperty("finish_reason")
        private String finishReason;
        
        // 简化的getter/setter
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        
        public Map<String, String> getMessage() { return message; }
        public void setMessage(Map<String, String> message) { this.message = message; }
        
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens") 
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        
        // 简化的getter/setter
        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        
        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    }
    
    // 主要的getter/setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    
    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
    
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }
    
    /**
     * 核心方法：获取AI返回的内容
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().get("content");
        }
        return null;
    }
} 