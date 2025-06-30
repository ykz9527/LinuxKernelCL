package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 概念关系分析结果DTO
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "概念关系分析结果")
public class ConceptRelationshipResultDTO {
    
    @Schema(description = "核心概念")
    private String coreConcept;
    
    @Schema(description = "关联概念列表")
    private List<RelatedConcept> relatedConcepts;
    
    @Schema(description = "AI分析的关系总结")
    private String relationshipSummary;
    
    @Schema(description = "分析置信度(0-1)")
    private Double confidenceScore;
    
    @Schema(description = "涉及的feature数量")
    private Integer totalFeatures;
    
    @Schema(description = "发现的关联概念数量")
    private Integer totalRelatedConcepts;
    
    public ConceptRelationshipResultDTO() {
        this.relatedConcepts = new ArrayList<>();
        this.confidenceScore = 0.0;
        this.totalFeatures = 0;
        this.totalRelatedConcepts = 0;
    }
    
    /**
     * 关联概念内部类
     */
    @Schema(description = "关联概念信息")
    public static class RelatedConcept {
        
        @Schema(description = "关联概念名称")
        private String conceptName;
        
        @Schema(description = "与核心概念的关系类型")
        private String relationshipType;
        
        @Schema(description = "关系强度(0-1)")
        private Double relationshipStrength;
        
        @Schema(description = "关系描述")
        private String relationshipDescription;
        
        @Schema(description = "共同的feature数量")
        private Integer sharedFeatures;
        
        @Schema(description = "相关feature描述列表")
        private List<String> featureDescriptions;
        
        public RelatedConcept() {
            this.featureDescriptions = new ArrayList<>();
        }
        
        public RelatedConcept(String conceptName, String relationshipType, Double relationshipStrength, String relationshipDescription) {
            this();
            this.conceptName = conceptName;
            this.relationshipType = relationshipType;
            this.relationshipStrength = relationshipStrength;
            this.relationshipDescription = relationshipDescription;
        }
        
        // Getters and Setters
        public String getConceptName() {
            return conceptName;
        }
        
        public void setConceptName(String conceptName) {
            this.conceptName = conceptName;
        }
        
        public String getRelationshipType() {
            return relationshipType;
        }
        
        public void setRelationshipType(String relationshipType) {
            this.relationshipType = relationshipType;
        }
        
        public Double getRelationshipStrength() {
            return relationshipStrength;
        }
        
        public void setRelationshipStrength(Double relationshipStrength) {
            this.relationshipStrength = relationshipStrength;
        }
        
        public String getRelationshipDescription() {
            return relationshipDescription;
        }
        
        public void setRelationshipDescription(String relationshipDescription) {
            this.relationshipDescription = relationshipDescription;
        }
        
        public Integer getSharedFeatures() {
            return sharedFeatures;
        }
        
        public void setSharedFeatures(Integer sharedFeatures) {
            this.sharedFeatures = sharedFeatures;
        }
        
        public List<String> getFeatureDescriptions() {
            return featureDescriptions;
        }
        
        public void setFeatureDescriptions(List<String> featureDescriptions) {
            this.featureDescriptions = featureDescriptions;
        }
        
        public void addFeatureDescription(String featureDescription) {
            if (this.featureDescriptions == null) {
                this.featureDescriptions = new ArrayList<>();
            }
            this.featureDescriptions.add(featureDescription);
        }
    }
    
    // Getters and Setters
    public String getCoreConcept() {
        return coreConcept;
    }
    
    public void setCoreConcept(String coreConcept) {
        this.coreConcept = coreConcept;
    }
    
    public List<RelatedConcept> getRelatedConcepts() {
        return relatedConcepts;
    }
    
    public void setRelatedConcepts(List<RelatedConcept> relatedConcepts) {
        this.relatedConcepts = relatedConcepts;
    }
    
    public String getRelationshipSummary() {
        return relationshipSummary;
    }
    
    public void setRelationshipSummary(String relationshipSummary) {
        this.relationshipSummary = relationshipSummary;
    }
    
    public Double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public Integer getTotalFeatures() {
        return totalFeatures;
    }
    
    public void setTotalFeatures(Integer totalFeatures) {
        this.totalFeatures = totalFeatures;
    }
    
    public Integer getTotalRelatedConcepts() {
        return totalRelatedConcepts;
    }
    
    public void setTotalRelatedConcepts(Integer totalRelatedConcepts) {
        this.totalRelatedConcepts = totalRelatedConcepts;
    }
    
    public void addRelatedConcept(RelatedConcept relatedConcept) {
        if (this.relatedConcepts == null) {
            this.relatedConcepts = new ArrayList<>();
        }
        this.relatedConcepts.add(relatedConcept);
        this.totalRelatedConcepts = this.relatedConcepts.size();
    }
} 