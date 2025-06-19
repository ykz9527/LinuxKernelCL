package com.cs.api.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 三元组搜索请求DTO
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "三元组搜索请求参数")
public class TripleSearchRequestDTO {
    
    @NotBlank(message = "概念不能为空")
    @Schema(description = "要搜索的概念", example = "touch driver", required = true)
    private String concept;
    
    @Schema(description = "上下文信息", example = "Linux内核驱动相关")
    private String context;
    
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
    
    @Override
    public String toString() {
        return String.format("TripleSearchRequestDTO{concept='%s', context='%s'}", concept, context);
    }
} 