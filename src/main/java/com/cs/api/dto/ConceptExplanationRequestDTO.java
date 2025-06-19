package com.cs.api.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 概念解释请求DTO
 *
 * @author YK
 * @since 1.0.0
 */
public class ConceptExplanationRequestDTO {

    @NotBlank(message = "概念不能为空")
    @Schema(description = "要搜索的概念", example = "process")
    private String concept;

    @Schema(description = "概念相关的上下文信息", example = "In Linux, a process is an instance of a running program.")
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
        return "ConceptExplanationRequestDTO{" +
                "concept='" + concept + '\'' +
                ", context='" + context + '\'' +
                '}';
    }
} 