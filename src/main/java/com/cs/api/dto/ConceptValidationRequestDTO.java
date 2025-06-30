package com.cs.api.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 概念验证请求DTO
 * 用于验证输入的概念是否存在于概念列表中
 *
 * @author YK
 * @since 1.0.0
 */
public class ConceptValidationRequestDTO {

    @NotBlank(message = "概念不能为空")
    @Schema(description = "要验证的概念", example = "memory allocation", required = true)
    private String concept;

    @Schema(description = "概念相关的上下文信息，用于精确匹配多个同名概念", 
            example = "Linux内核内存管理中的内存分配机制")
    private String context;

    // Getters and Setters

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
        return "ConceptValidationRequestDTO{" +
                "concept='" + concept + '\'' +
                ", context='" + context + '\'' +
                '}';
    }
} 