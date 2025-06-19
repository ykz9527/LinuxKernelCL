package com.cs.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
/**
 * 代码追溯响应 DTO
 * 包含方法名及其对应的演化历史信息
 * 
 * @author YK
 * @since 1.0.0
 */
@Schema(description = "代码追溯响应结果")
public class CodeTraceResponseDTO {

    @Schema(description = "方法追溯信息，键为方法签名，值为该方法的演化历史列表")
    private CodeTraceResultDTO commitHistory;

    @Schema(description = "追溯成功标识", example = "true")
    private boolean success;

    @Schema(description = "文件路径", example = "kernel/sched/fair.c")
    private String filePath;

    @Schema(description = "代码片段")
    private String codeSnippet;

    @Schema(description = "起始行号", example = "6415")
    private Integer startLine;

    @Schema(description = "结束行号", example = "6433")
    private Integer endLine;

    @Schema(description = "代码解释说明")
    private String explanation;


    @Schema(description = "错误消息（如果有）")
    private String errorMessage;

    // 默认构造函数
    public CodeTraceResponseDTO() {
        this.success = true;
    }

    // 构造函数
    public CodeTraceResponseDTO(CodeTraceResultDTO commitHistory) {
        this.commitHistory = commitHistory;
        this.success = true;
    }

    // 错误响应构造函数
    public CodeTraceResponseDTO(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
    }

    // Getter 和 Setter 方法
    public CodeTraceResultDTO getCommitHistory() {
        return commitHistory;
    }

    public void setCommitHistory(CodeTraceResultDTO commitHistory) {
        this.commitHistory = commitHistory;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    @Override
    public String toString() {
        return "CodeTraceResponseDTO{" +
                "commitHistory=" + commitHistory +
                ", success=" + success +
                ", filePath='" + filePath + '\'' +
                ", codeSnippet='" + codeSnippet + '\'' +
                ", startLine=" + startLine +
                ", endLine=" + endLine +
                ", explanation='" + explanation + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
} 