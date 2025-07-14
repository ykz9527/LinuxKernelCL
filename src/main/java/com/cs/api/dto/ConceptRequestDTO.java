package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 概念请求DTO
 * 统一的概念相关请求参数，用于所有基于概念ID的操作
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "概念请求参数")
public class ConceptRequestDTO {

    @NotNull(message = "概念ID不能为空")
    @Schema(description = "概念ID", example = "1", required = true)
    private Long eid;

    @Schema(description = "代码版本标签", example = "v6.14")
    private String version;

    @Schema(description = "分析深度级别(1-3)，默认为2", example = "2")
    private Integer analysisDepth = 2;

    public ConceptRequestDTO() {
    }

    public ConceptRequestDTO(Long eid) {
        this.eid = eid;
    }

    public ConceptRequestDTO(Long eid, String version) {
        this.eid = eid;
        this.version = version;
    }

    public ConceptRequestDTO(Long eid, String version, Integer analysisDepth) {
        this.eid = eid;
        this.version = version;
        this.analysisDepth = analysisDepth;
    }

    public Long getEid() {
        return eid;
    }

    public void setEid(Long eid) {
        this.eid = eid;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getAnalysisDepth() {
        return analysisDepth;
    }

    public void setAnalysisDepth(Integer analysisDepth) {
        this.analysisDepth = analysisDepth;
    }

    @Override
    public String toString() {
        return "ConceptRequestDTO{" +
                "eid=" + eid +
                ", version='" + version + '\'' +
                ", analysisDepth=" + analysisDepth +
                '}';
    }
} 