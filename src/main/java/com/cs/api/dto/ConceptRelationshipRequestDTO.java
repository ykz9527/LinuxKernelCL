package com.cs.api.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 概念关系分析请求DTO
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "概念关系分析请求参数")
public class ConceptRelationshipRequestDTO {
    
    @NotBlank(message = "概念不能为空")
    @Schema(description = "要分析的核心概念", example = "touch driver", required = true)
    private String concept;
    
    @Schema(description = "上下文信息，有助于提高分析准确性", example = "Linux内核驱动相关")
    private String context;
    
    @Schema(description = "分析深度级别(1-3)，默认为2", example = "2")
    private Integer analysisDepth = 2;
    
    public String getConcept() {
        return concept;
    }
    
    public void setConcept(String concept) {
        this.concept = concept;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public Integer getAnalysisDepth() {
        return analysisDepth;
    }
    
    public void setAnalysisDepth(Integer analysisDepth) {
        this.analysisDepth = analysisDepth;
    }
    
    @Override
    public String toString() {
        return String.format("ConceptRelationshipRequestDTO{concept='%s', context='%s', analysisDepth=%d}", 
            concept, context, analysisDepth);
    }
} 