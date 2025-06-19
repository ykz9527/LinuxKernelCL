package com.cs.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 内核特征数据DTO，用于解析JSONL文件
 * 
 * @author YK
 * @since 1.0.0
 */
public class KernelFeatureDTO {
    
    @JsonProperty("feature_id")
    private Integer featureId;
    
    @JsonProperty("feature")
    private Feature feature;
    
    @JsonProperty("extraction_result")
    private ExtractionResult extractionResult;
    
    public static class Feature {
        @JsonProperty("feature_id")
        private Integer featureId;
        
        @JsonProperty("h1")
        private String h1;
        
        @JsonProperty("h2")
        private String h2;
        
        @JsonProperty("feature_description")
        private String featureDescription;
        
        @JsonProperty("version")
        private String version;
        
        // Getters and Setters
        public Integer getFeatureId() {
            return featureId;
        }
        
        public void setFeatureId(Integer featureId) {
            this.featureId = featureId;
        }
        
        public String getH1() {
            return h1;
        }
        
        public void setH1(String h1) {
            this.h1 = h1;
        }
        
        public String getH2() {
            return h2;
        }
        
        public void setH2(String h2) {
            this.h2 = h2;
        }
        
        public String getFeatureDescription() {
            return featureDescription;
        }
        
        public void setFeatureDescription(String featureDescription) {
            this.featureDescription = featureDescription;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
    }
    
    public static class ExtractionResult {
        @JsonProperty("filtered_entities")
        private List<String> filteredEntities;
        
        public List<String> getFilteredEntities() {
            return filteredEntities;
        }
        
        public void setFilteredEntities(List<String> filteredEntities) {
            this.filteredEntities = filteredEntities;
        }
    }
    
    // Main getters and setters
    public Integer getFeatureId() {
        return featureId;
    }
    
    public void setFeatureId(Integer featureId) {
        this.featureId = featureId;
    }
    
    public Feature getFeature() {
        return feature;
    }
    
    public void setFeature(Feature feature) {
        this.feature = feature;
    }
    
    public ExtractionResult getExtractionResult() {
        return extractionResult;
    }
    
    public void setExtractionResult(ExtractionResult extractionResult) {
        this.extractionResult = extractionResult;
    }
} 