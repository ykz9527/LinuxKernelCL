package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 代码搜索结果DTO
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "代码搜索结果")
public class CodeSearchResultDTO {

    @Schema(description = "文件路径", example = "kernel/sched/fair.c")
    private String filePath;

    @Schema(description = "元素名称", example = "fair_sched_class")
    private String elementName;

    @Schema(description = "代码片段")
    private String codeSnippet;

    @Schema(description = "起始行号", example = "6415")
    private Integer startLine;

    @Schema(description = "目标行号", example = "6415")
    private Integer targetLine;

    @Schema(description = "结束行号", example = "6433")
    private Integer endLine;

    @Schema(description = "代码解释说明")
    private String explanation;

    @Schema(description = "版本信息", example = "v6.14")
    private String version;

    @Schema(description = "类型", example = "function")
    private String type;

    public CodeSearchResultDTO() {
    }

    public CodeSearchResultDTO(String filePath, String elementName, String codeSnippet, Integer startLine, 
                               Integer targetLine, Integer endLine, String explanation, String version, String type) {
        this.filePath = filePath;
        this.elementName = elementName;
        this.codeSnippet = codeSnippet;
        this.startLine = startLine;
        this.targetLine = targetLine;
        this.endLine = endLine;
        this.explanation = explanation;
        this.version = version;
        this.type = type;
    }

    // 向后兼容的6参数构造函数
    public CodeSearchResultDTO(String filePath, String codeSnippet, Integer startLine,Integer targetLine, 
                               Integer endLine, String explanation, String version) {
        this.filePath = filePath;
        this.codeSnippet = codeSnippet;
        this.startLine = startLine;
        this.targetLine = targetLine;
        this.endLine = endLine;
        this.explanation = explanation;
        this.version = version;
        this.type = null; // 默认值
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getTargetLine() {
        return targetLine;
    }

    public void setTargetLine(Integer targetLine) {
        this.targetLine = targetLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "CodeSearchResultDTO{" +
                "filePath='" + filePath + '\'' +
                ", elementName='" + elementName + '\'' +
                ", codeSnippet='" + codeSnippet + '\'' +
                ", startLine=" + startLine +
                ", targetLine=" + targetLine +
                ", endLine=" + endLine +
                ", explanation='" + explanation + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
} 