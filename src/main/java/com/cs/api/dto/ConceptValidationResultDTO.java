package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 概念验证结果DTO
 * 返回概念验证的结果信息
 *
 * @author YK
 * @since 1.0.0
 */
public class ConceptValidationResultDTO {

    @Schema(description = "概念ID")
    private Long eid;

    @Schema(description = "验证的概念")
    private String concept;

    @Schema(description = "是否找到匹配的概念")
    private boolean exists;

    @Schema(description = "匹配的概念数量")
    private int matchCount;

    @Schema(description = "最佳匹配的概念名称（如果存在多个匹配）")
    private String bestMatchConcept;

    @Schema(description = "最佳匹配的概念定义")
    private String bestMatchDefinition;

    @Schema(description = "验证详情说明")
    private String details;

    // 构造函数
    public ConceptValidationResultDTO() {}

    public ConceptValidationResultDTO(Long eid, String concept, boolean exists, int matchCount, 
                                     String bestMatchConcept, String bestMatchDefinition, 
                                     String details) {
        this.eid = eid;
        this.concept = concept;
        this.exists = exists;
        this.matchCount = matchCount;
        this.bestMatchConcept = bestMatchConcept;
        this.bestMatchDefinition = bestMatchDefinition;
        this.details = details;
    }

    // Getters and Setters

    public Long getEid() {
        return eid;
    }

    public void setEid(Long eid) {
        this.eid = eid;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

    public String getBestMatchConcept() {
        return bestMatchConcept;
    }

    public void setBestMatchConcept(String bestMatchConcept) {
        this.bestMatchConcept = bestMatchConcept;
    }

    public String getBestMatchDefinition() {
        return bestMatchDefinition;
    }

    public void setBestMatchDefinition(String bestMatchDefinition) {
        this.bestMatchDefinition = bestMatchDefinition;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "ConceptValidationResultDTO{" +
                "eid=" + eid +
                "concept='" + concept + '\'' +
                ", exists=" + exists +
                ", matchCount=" + matchCount +
                ", bestMatchConcept='" + bestMatchConcept + '\'' +
                ", bestMatchDefinition='" + bestMatchDefinition + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
} 