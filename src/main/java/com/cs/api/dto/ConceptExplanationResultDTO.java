package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 概念解释结果DTO
 *
 * @author YK
 * @since 1.0.0
 */
public class ConceptExplanationResultDTO {

    @Schema(description = "查询的概念")
    private String concept;

    @Schema(description = "找到的概念解释")
    private String explanation;

    @Schema(description = "找到的概念解释的来源")
    private String source;

    public ConceptExplanationResultDTO(String concept, String explanation, String source) {
        this.concept = concept;
        this.explanation = explanation;
        this.source = source;
    }

    // Getters and Setters

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "ConceptExplanationResultDTO{" +
                "concept='" + concept + '\'' +
                ", explanation='" + explanation + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
} 