package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 三元组搜索结果DTO
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "三元组搜索结果")
public class TripleSearchResultDTO {
    
    @Schema(description = "搜索的概念")
    private String concept;
    
    @Schema(description = "匹配的特征ID")
    private Integer featureId;
    
    @Schema(description = "特征描述")
    private String featureDescription;
    
    @Schema(description = "版本信息")
    private String version;
    
    @Schema(description = "分类信息(h1)")
    private String category;
    
    @Schema(description = "子分类信息(h2)")
    private String subCategory;
    
    @Schema(description = "AI分析提取的三元组")
    private List<String> triples;
    
    public String getConcept() {
        return concept;
    }
    
    public void setConcept(String concept) {
        this.concept = concept;
    }
    
    public Integer getFeatureId() {
        return featureId;
    }
    
    public void setFeatureId(Integer featureId) {
        this.featureId = featureId;
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getSubCategory() {
        return subCategory;
    }
    
    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
    
    public List<String> getTriples() {
        return triples;
    }
    
    public void setTriples(List<String> triples) {
        this.triples = triples;
    }

    public String addTriple(String triple) {
        if (triples == null) {
            triples = new ArrayList<>();
        }
        triples.add(triple);
        return triple;
    }
} 